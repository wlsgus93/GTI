package com.gametrend.insight.application.compare;

import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.economics.EconomicsQueryService;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.game.GameQueryService;
import com.gametrend.insight.application.game.PlayerInsight;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * P3 게임 비교 서비스 — 여러 게임의 detail/player/economics를 병렬 조립 (W1 Virtual Threads 패턴 재사용).
 *
 * <p>특징:
 * <ul>
 *   <li>입력: 2~5 game ID (DoS 방어 — 너무 많은 동시 LLM/DB 부하 차단)
 *   <li>병렬: {@code newVirtualThreadPerTaskExecutor()} — N개 동시, wall-clock = max latency
 *   <li>Graceful: 미존재 게임은 {@code missingGameIds}에 별도 표시. 전체 실패 X
 *   <li>중복 ID dedupe — 호출자가 잘못 보내도 결과는 distinct
 * </ul>
 */
@Service
public class GameCompareService {

    private static final Logger log = LoggerFactory.getLogger(GameCompareService.class);

    public static final int MIN_IDS = 2;
    public static final int MAX_IDS = 5;

    private final GameQueryService gameQueryService;
    private final EconomicsQueryService economicsQueryService;

    public GameCompareService(GameQueryService gameQueryService, EconomicsQueryService economicsQueryService) {
        this.gameQueryService = gameQueryService;
        this.economicsQueryService = economicsQueryService;
    }

    public CompareResult compare(List<Long> rawIds) {
        // 1. 검증 + dedupe (요청 순서 유지)
        List<Long> ids = rawIds.stream().distinct().toList();
        if (ids.size() < MIN_IDS) {
            throw new IllegalArgumentException("compare requires at least " + MIN_IDS + " distinct ids, got " + ids.size());
        }
        if (ids.size() > MAX_IDS) {
            throw new IllegalArgumentException("compare supports up to " + MAX_IDS + " ids, got " + ids.size());
        }

        // 2. Virtual Threads로 병렬 조회 (W1 IngestionOrchestrator 패턴)
        long t0 = System.nanoTime();
        Map<Long, CompareItem> okMap = new LinkedHashMap<>();
        List<Long> missing = new ArrayList<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Map<Long, CompletableFuture<FetchResult>> futures = ids.stream()
                    .collect(Collectors.toMap(
                            id -> id,
                            id -> CompletableFuture.supplyAsync(() -> fetchOne(id), executor),
                            (a, b) -> a,
                            LinkedHashMap::new));

            for (Long id : ids) {
                try {
                    FetchResult r = futures.get(id).join();
                    if (r.item() != null) {
                        okMap.put(id, r.item());
                    } else {
                        missing.add(id);
                    }
                } catch (Exception e) {
                    log.warn("Compare fetch failed for gameId={}: {}", id, e.getMessage());
                    missing.add(id);
                }
            }
        }

        long wallClockMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("Compare {} games — wall-clock={}ms, ok={}, missing={}",
                ids.size(), wallClockMs, okMap.size(), missing.size());

        // 요청 순서 유지하여 items 빌드
        List<CompareItem> items = ids.stream()
                .map(okMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();

        return new CompareResult(items, List.copyOf(missing), wallClockMs, Instant.now());
    }

    private FetchResult fetchOne(long gameId) {
        try {
            GameDetailItem detail = gameQueryService.getDetail(gameId);
            PlayerInsight player = gameQueryService.getPlayerInsight(gameId);
            EconomicsInsight econ = economicsQueryService.getEconomicsInsight(gameId);
            return new FetchResult(toCompareItem(detail, player, econ), null);
        } catch (GameNotFoundException e) {
            return new FetchResult(null, e);
        }
    }

    private static CompareItem toCompareItem(GameDetailItem d, PlayerInsight p, EconomicsInsight e) {
        Integer mentionsTotal = p.mentions().isEmpty()
                ? null
                : p.mentions().stream().mapToInt(PlayerInsight.MentionByPlatform::mentionCount).sum();
        Double reviewPct = p.players() == null ? null : p.players().reviewScorePercent();
        Long ownersMid = e.revenue() == null ? null : e.revenue().ownersMid();
        var priceUsd = e.revenue() == null ? null : e.revenue().priceUsd();
        var net = e.revenue() == null ? null : e.revenue().developerNet();

        return new CompareItem(
                d.id(),
                d.steamAppId(),
                d.name(),
                d.genres(),
                d.coverImageUrl(),
                d.latestCcu(),
                d.ccuDeltaPct(),
                p.twitchViewers(),
                mentionsTotal,
                reviewPct,
                ownersMid,
                priceUsd,
                net,
                e.confidence() == null ? null : e.confidence().name());
    }

    private record FetchResult(CompareItem item, Throwable error) {}
}
