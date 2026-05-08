package com.gametrend.insight.application.insight;

import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.economics.EconomicsQueryService;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.GameQueryService;
import com.gametrend.insight.application.game.PlayerInsight;
import com.gametrend.insight.domain.insight.Analysis;
import com.gametrend.insight.domain.insight.AnalysisKind;
import com.gametrend.insight.domain.insight.Persona;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.insight.AnalysisJpaEntity;
import com.gametrend.insight.infrastructure.persistence.insight.AnalysisJpaRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P2 AI 인사이트 — 4 단계 fallback 체인 (W3 D1 회복 탄력성 강화).
 *
 * <pre>
 *   L1: Redis hot cache       (~1ms)         → cached=true,  stale=false
 *   L2: DB fresh (TTL 24h 내)  (~10ms)        → cached=true,  stale=false  + Redis 채움
 *   L3: LLM 호출 (CB 보호)     (~1-3s)         → cached=false, stale=false  + DB 영속화 + Redis
 *   L4: DB stale (TTL 무관)    (~10ms)        → cached=true,  stale=true   (Anthropic 장애 시 graceful)
 *   L5: 모두 실패              → LlmUnavailableException → 503 + Retry-After
 * </pre>
 *
 * <p>회로 차단기 — `llm` 인스턴스. 50% 실패율 → OPEN 60s. OPEN 중에는 LLM 호출 자체 차단되어 즉시 L4로.
 */
@Service
public class InsightService {

    private static final Logger log = LoggerFactory.getLogger(InsightService.class);

    public static final Duration TTL = Duration.ofHours(24);
    public static final String CB_NAME = "llm";
    private static final String CACHE_PREFIX = "insight:game:";

    private final AnalysisJpaRepository analysisRepo;
    private final GameQueryService gameQueryService;
    private final EconomicsQueryService economicsQueryService;
    private final LlmClient llmClient;
    private final RedisCacheTemplate redisCache;
    private final CircuitBreaker llmCircuitBreaker;
    private final HallucinationValidator hallucinationValidator;

    public InsightService(
            AnalysisJpaRepository analysisRepo,
            GameQueryService gameQueryService,
            EconomicsQueryService economicsQueryService,
            LlmClient llmClient,
            RedisCacheTemplate redisCache,
            CircuitBreakerRegistry circuitBreakerRegistry,
            HallucinationValidator hallucinationValidator) {
        this.analysisRepo = analysisRepo;
        this.gameQueryService = gameQueryService;
        this.economicsQueryService = economicsQueryService;
        this.llmClient = llmClient;
        this.redisCache = redisCache;
        this.llmCircuitBreaker = circuitBreakerRegistry.circuitBreaker(CB_NAME);
        this.hallucinationValidator = hallucinationValidator;
    }

    /** Default 페르소나 (인디) 호출 — 기존 호환. */
    public GameInsight getOrGenerate(long gameId) {
        return getOrGenerate(gameId, Persona.DEFAULT);
    }

    /** 페르소나 분기 호출 — W5 D2 도입. promptVersion + 캐시 키 페르소나별 분리. */
    public GameInsight getOrGenerate(long gameId, Persona persona) {
        Instant now = Instant.now();
        String promptVersion = PromptBuilder.version(persona);
        String cacheKey = cacheKey(gameId, persona);

        // L1: Redis hot cache
        Optional<GameInsight> hot = redisCache.get(cacheKey, GameInsight.class);
        if (hot.isPresent()) {
            log.debug("Insight L1 Redis HIT — gameId={}, persona={}", gameId, persona);
            return withFlags(hot.get(), true, false);
        }

        // L2: DB fresh
        Optional<AnalysisJpaEntity> fresh = analysisRepo.findLatestFresh(
                gameId, AnalysisKind.INSIGHT_BRIEF, promptVersion, now);
        if (fresh.isPresent()) {
            log.debug("Insight L2 DB-fresh HIT — gameId={}, persona={}", gameId, persona);
            GameInsight dto = toDto(fresh.get().toDomain(), true, false);
            redisCache.put(cacheKey, dto, TTL);
            return dto;
        }

        // L3: LLM 호출 (CircuitBreaker 보호)
        try {
            return callLlmAndPersist(gameId, persona, now, cacheKey);
        } catch (CallNotPermittedException e) {
            log.warn("LLM circuit OPEN — falling back to stale, gameId={}, persona={}", gameId, persona);
            return staleOrFail(gameId, persona, e);
        } catch (Exception e) {
            log.warn("LLM call failed: {} — falling back to stale, gameId={}, persona={}",
                    e.getMessage(), gameId, persona);
            return staleOrFail(gameId, persona, e);
        }
    }

    private GameInsight callLlmAndPersist(long gameId, Persona persona, Instant now, String cacheKey) {
        GameDetailItem detail = gameQueryService.getDetail(gameId);
        PlayerInsight player = gameQueryService.getPlayerInsight(gameId);
        EconomicsInsight econ = economicsQueryService.getEconomicsInsight(gameId);
        InsightContext ctx = buildContext(detail, player, econ);

        String system = PromptBuilder.systemPrompt(persona);
        String user = PromptBuilder.userPrompt(ctx);
        log.info("Insight L3 LLM call — gameId={}, persona={}, promptLen={}", gameId, persona, user.length());

        // 1차 LLM 호출
        LlmResponse response = llmCircuitBreaker.executeSupplier(
                () -> llmClient.complete(system, user, PromptBuilder.MAX_TOKENS));

        // W7 D1 — 할루시네이션 검증. fabricated 수치 검출 시 재호출 1회.
        var validation = hallucinationValidator.validate(response.content(), ctx);
        if (!validation.valid()) {
            log.warn("Hallucination detected (1st call) — gameId={}, persona={}, fabricated={}",
                    gameId, persona, validation.fabricatedNumbers());
            // 재호출 1회 (system prompt에 강조 추가)
            String retrySystem = system + "\n\n## 추가 안전 규칙 (재호출)\n"
                    + "이전 응답에 USER_DATA에 없는 수치를 만들어냈다. 절대 금지. "
                    + "수치는 USER_DATA의 [최근 지표] / [Economics 추정] 항목에서만 인용.";
            LlmResponse retry = llmCircuitBreaker.executeSupplier(
                    () -> llmClient.complete(retrySystem, user, PromptBuilder.MAX_TOKENS));
            var retryValidation = hallucinationValidator.validate(retry.content(), ctx);
            if (!retryValidation.valid()) {
                log.error("Hallucination persists after retry — gameId={}, persona={}, fabricated={}",
                        gameId, persona, retryValidation.fabricatedNumbers());
                throw new HallucinationDetectedException(gameId, retryValidation.fabricatedNumbers());
            }
            response = retry;
            log.info("Hallucination resolved after retry — gameId={}, persona={}", gameId, persona);
        }

        Analysis saved = persist(gameId, persona, response, now);
        GameInsight dto = toDto(saved, false, false);
        redisCache.put(cacheKey, dto, TTL);
        return dto;
    }

    @Transactional(readOnly = true)
    GameInsight staleOrFail(long gameId, Persona persona, Throwable cause) {
        gameQueryService.getDetail(gameId);

        String promptVersion = PromptBuilder.version(persona);
        Optional<AnalysisJpaEntity> stale = analysisRepo.findLatestAny(
                gameId, AnalysisKind.INSIGHT_BRIEF, promptVersion);
        if (stale.isPresent()) {
            log.info("Insight L4 stale fallback — gameId={}, persona={}, age={}",
                    gameId, persona, Duration.between(stale.get().getCreatedAt(), Instant.now()));
            return toDto(stale.get().toDomain(), true, true);
        }
        log.error("Insight L5 — no stale history, gameId={}, persona={}", gameId, persona);
        throw new LlmUnavailableException(gameId, cause);
    }

    private InsightContext buildContext(GameDetailItem d, PlayerInsight p, EconomicsInsight e) {
        Long ownersMid = e.revenue() == null ? null : e.revenue().ownersMid();
        var price = e.revenue() == null ? null : e.revenue().priceUsd();
        var net = e.revenue() == null ? null : e.revenue().developerNet();
        Double v2p = e.unitEconomics() == null ? null : e.unitEconomics().viewToPlayRatio();
        Integer mentionsTotal = p.mentions().stream().mapToInt(PlayerInsight.MentionByPlatform::mentionCount).sum();
        if (p.mentions().isEmpty()) mentionsTotal = null;

        return new InsightContext(
                d.name(),
                d.genres(),
                d.developer(),
                d.latestCcu(),
                d.ccuDeltaPct(),
                p.twitchViewers(),
                mentionsTotal,
                p.players() == null ? null : p.players().reviewScorePercent(),
                ownersMid,
                price,
                net,
                v2p,
                e.confidence() == null ? null : e.confidence().name());
    }

    @Transactional
    Analysis persist(long gameId, Persona persona, LlmResponse response, Instant now) {
        Analysis domain = new Analysis(
                null,
                gameId,
                AnalysisKind.INSIGHT_BRIEF,
                PromptBuilder.version(persona),
                response.content(),
                response.promptTokens(),
                response.completionTokens(),
                response.totalTokens(),
                response.model(),
                now,
                now.plus(TTL));
        return analysisRepo.save(AnalysisJpaEntity.from(domain)).toDomain();
    }

    private static GameInsight toDto(Analysis a, boolean cached, boolean stale) {
        return new GameInsight(
                a.gameId(),
                a.content(),
                a.promptVersion(),
                a.totalTokens(),
                a.model(),
                cached,
                stale,
                a.createdAt(),
                a.expiresAt());
    }

    private static GameInsight withFlags(GameInsight src, boolean cached, boolean stale) {
        return new GameInsight(
                src.gameId(), src.summary(), src.promptVersion(), src.totalTokens(),
                src.model(), cached, stale, src.generatedAt(), src.expiresAt());
    }

    private static String cacheKey(long gameId, Persona persona) {
        return CACHE_PREFIX + gameId + ":" + PromptBuilder.version(persona);
    }

    // ====================================================================================
    // Multi-persona (W6 D2) — Virtual Threads 병렬 호출 + 페르소나별 fallback chain
    // ====================================================================================

    /** Multi-persona 동시 호출 최대 (DoS 방어 — 4 페르소나 모두 합법). */
    public static final int MAX_PERSONAS = 4;

    /**
     * 여러 페르소나를 동시 호출 — 병렬 LLM (Virtual Threads).
     *
     * <p>각 페르소나는 기존 {@link #getOrGenerate(long, Persona)} 재사용 → 캐시 isolation 그대로.
     * Wall-clock = max(per-persona latency). 4 페르소나 모두 L1 hit 시 ~2ms.
     *
     * @param personas 1~4 페르소나 (중복 dedupe, 순서 유지)
     * @throws IllegalArgumentException 0개 또는 4개 초과
     */
    public MultiPersonaInsight getOrGenerateMulti(long gameId, List<Persona> personas) {
        if (personas == null || personas.isEmpty()) {
            throw new IllegalArgumentException("personas must not be empty");
        }
        // dedupe + 순서 유지
        List<Persona> unique = personas.stream().distinct().toList();
        if (unique.size() > MAX_PERSONAS) {
            throw new IllegalArgumentException(
                    "max " + MAX_PERSONAS + " personas, got " + unique.size());
        }

        long t0 = System.nanoTime();
        Map<Persona, MultiPersonaInsight.Perspective> result = new LinkedHashMap<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<Persona, CompletableFuture<GameInsight>> futures = new LinkedHashMap<>();
            for (Persona p : unique) {
                futures.put(p, CompletableFuture.supplyAsync(
                        () -> getOrGenerate(gameId, p), executor));
            }
            for (Persona p : unique) {
                GameInsight gi = futures.get(p).join();
                result.put(p, MultiPersonaInsight.Perspective.from(gi, p));
            }
        }

        long wallClockMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("Multi-persona insight — gameId={}, personas={}, wall-clock={}ms",
                gameId, unique, wallClockMs);

        return new MultiPersonaInsight(gameId, List.copyOf(result.values()), wallClockMs, Instant.now());
    }
}
