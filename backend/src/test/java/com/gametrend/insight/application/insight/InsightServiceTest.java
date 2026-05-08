package com.gametrend.insight.application.insight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.economics.EconomicsQueryService;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.GameQueryService;
import com.gametrend.insight.application.game.PlayerInsight;
import com.gametrend.insight.domain.insight.AnalysisKind;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.insight.AnalysisJpaEntity;
import com.gametrend.insight.infrastructure.persistence.insight.AnalysisJpaRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * 4단계 fallback 체인 검증 (W3 D1):
 * L1 Redis hot → L2 DB fresh → L3 LLM call → L4 stale → L5 throw.
 */
@ExtendWith(MockitoExtension.class)
class InsightServiceTest {

    @Mock AnalysisJpaRepository analysisRepo;
    @Mock GameQueryService gameQueryService;
    @Mock EconomicsQueryService economicsQueryService;
    @Mock LlmClient llmClient;
    @Mock RedisCacheTemplate redisCache;

    private CircuitBreakerRegistry cbRegistry;
    private InsightService service;

    @BeforeEach
    void setUp() {
        // 테스트용 CB — 즉시 trip되지 않게 큰 임계 + 짧은 OPEN 시간
        var config = CircuitBreakerConfig.custom()
                .slidingWindowSize(100)
                .minimumNumberOfCalls(100)
                .failureRateThreshold(99.9f)
                .waitDurationInOpenState(Duration.ofSeconds(1))
                .build();
        cbRegistry = CircuitBreakerRegistry.of(config);
        // "llm" 이름 등록
        cbRegistry.circuitBreaker(InsightService.CB_NAME);

        service = new InsightService(
                analysisRepo, gameQueryService, economicsQueryService, llmClient, redisCache, cbRegistry,
                new HallucinationValidator());
    }

    @Nested
    @DisplayName("L1 — Redis hot cache")
    class L1Redis {
        @Test
        @DisplayName("Redis HIT — DB/LLM 호출 X, cached=true stale=false")
        void redisHit() {
            var hot = new GameInsight(
                    1L, "redis cached", "INSIGHT_V1", 100, "claude-opus-4-5",
                    false, false, // 저장 시점 flags (서비스가 재설정해야 함)
                    Instant.now(), Instant.now().plusSeconds(86400));
            when(redisCache.get(eq("insight:game:1:INSIGHT_V2_INDIE"), eq(GameInsight.class)))
                    .thenReturn(Optional.of(hot));

            GameInsight result = service.getOrGenerate(1L);

            assertThat(result.cached()).isTrue();
            assertThat(result.stale()).isFalse();
            assertThat(result.summary()).isEqualTo("redis cached");
            verify(analysisRepo, never()).findLatestFresh(any(), any(), any(), any());
            verify(llmClient, never()).complete(anyString(), anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("L2 — DB fresh cache")
    class L2DbFresh {
        @Test
        @DisplayName("Redis MISS + DB fresh HIT — LLM 호출 X, Redis 채움")
        void dbFreshHit_promotesToRedis() {
            when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
            when(analysisRepo.findLatestFresh(eq(1L), eq(AnalysisKind.INSIGHT_BRIEF),
                    eq(PromptBuilder.version(com.gametrend.insight.domain.insight.Persona.DEFAULT)), any(Instant.class)))
                    .thenReturn(Optional.of(freshEntity(1L, "db fresh", Instant.now())));

            GameInsight result = service.getOrGenerate(1L);

            assertThat(result.cached()).isTrue();
            assertThat(result.stale()).isFalse();
            assertThat(result.summary()).isEqualTo("db fresh");
            verify(llmClient, never()).complete(anyString(), anyString(), anyInt());
            verify(redisCache, times(1))
                    .put(eq("insight:game:1:INSIGHT_V2_INDIE"), any(GameInsight.class), eq(InsightService.TTL));
        }
    }

    @Nested
    @DisplayName("L3 — LLM call")
    class L3Llm {
        @Test
        @DisplayName("Redis miss + DB miss → LLM 호출 + 영속화 + Redis 채움, cached=false")
        void llmCall_persistAndCache() {
            when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
            when(analysisRepo.findLatestFresh(any(), any(), any(), any())).thenReturn(Optional.empty());

            wireContext();
            when(llmClient.complete(anyString(), anyString(), anyInt()))
                    .thenReturn(new LlmResponse("LLM 응답", 1000, 300, "claude-opus-4-5"));
            when(analysisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GameInsight result = service.getOrGenerate(1L);

            assertThat(result.cached()).isFalse();
            assertThat(result.stale()).isFalse();
            assertThat(result.totalTokens()).isEqualTo(1300);
            verify(analysisRepo, times(1)).save(any());
            verify(redisCache, times(1)).put(anyString(), any(GameInsight.class), eq(InsightService.TTL));
        }

        @Test
        @DisplayName("LLM 호출 시 회로 차단기 통계에 기록 — 정상 호출은 success로")
        void llmSuccess_circuitBreakerObserves() {
            when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
            when(analysisRepo.findLatestFresh(any(), any(), any(), any())).thenReturn(Optional.empty());
            wireContext();
            when(llmClient.complete(anyString(), anyString(), anyInt()))
                    .thenReturn(new LlmResponse("ok", 100, 50, "claude-opus-4-5"));
            when(analysisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.getOrGenerate(1L);

            var metrics = cbRegistry.circuitBreaker(InsightService.CB_NAME).getMetrics();
            assertThat(metrics.getNumberOfSuccessfulCalls()).isEqualTo(1);
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("L4 — Stale fallback")
    class L4Stale {
        @Test
        @DisplayName("LLM 실패 + DB stale 존재 → stale=true, cached=true")
        void llmFails_staleExists_returnsStale() {
            when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
            when(analysisRepo.findLatestFresh(any(), any(), any(), any())).thenReturn(Optional.empty());
            wireContext();
            when(llmClient.complete(anyString(), anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Anthropic timeout"));
            when(analysisRepo.findLatestAny(eq(1L), eq(AnalysisKind.INSIGHT_BRIEF), eq(PromptBuilder.version(com.gametrend.insight.domain.insight.Persona.DEFAULT))))
                    .thenReturn(Optional.of(freshEntity(1L, "이틀 전 분석", Instant.now().minusSeconds(2 * 86400))));

            GameInsight result = service.getOrGenerate(1L);

            assertThat(result.cached()).isTrue();
            assertThat(result.stale()).isTrue();
            assertThat(result.summary()).isEqualTo("이틀 전 분석");
            // LLM 실패는 회로 차단기에 기록됨
            var metrics = cbRegistry.circuitBreaker(InsightService.CB_NAME).getMetrics();
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("L5 — Hard failure")
    class L5HardFail {
        @Test
        @DisplayName("LLM 실패 + DB stale 없음 → LlmUnavailableException")
        void llmFails_noStale_throws() {
            when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
            when(analysisRepo.findLatestFresh(any(), any(), any(), any())).thenReturn(Optional.empty());
            wireContext();
            when(llmClient.complete(anyString(), anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Anthropic 502"));
            when(analysisRepo.findLatestAny(any(), any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOrGenerate(1L))
                    .isInstanceOf(LlmUnavailableException.class)
                    .hasMessageContaining("gameId=1");
        }

        @Test
        @DisplayName("LLM 실패 + 미존재 게임 → GameNotFoundException 우선 (LlmUnavailable 위에)")
        void llmFails_unknownGame_throwsNotFound() {
            when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
            when(analysisRepo.findLatestFresh(any(), any(), any(), any())).thenReturn(Optional.empty());
            // L3에서 getDetail이 호출되어 GameNotFoundException 던지면 → catch에서 staleOrFail
            // staleOrFail에서도 getDetail 호출 → 다시 GameNotFoundException 던짐 → propagate
            when(gameQueryService.getDetail(999L))
                    .thenThrow(new com.gametrend.insight.application.game.GameNotFoundException(999L));

            assertThatThrownBy(() -> service.getOrGenerate(999L))
                    .isInstanceOf(com.gametrend.insight.application.game.GameNotFoundException.class);
            verify(llmClient, never()).complete(anyString(), anyString(), anyInt());
        }
    }

    @Test
    @DisplayName("프롬프트 — 게임 데이터 정확히 포함 (Day 5 회귀 보존)")
    void promptContainsGameData() {
        when(redisCache.get(anyString(), eq(GameInsight.class))).thenReturn(Optional.empty());
        when(analysisRepo.findLatestFresh(any(), any(), any(), any())).thenReturn(Optional.empty());
        wireContext();
        when(llmClient.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new LlmResponse("ok", 100, 50, "claude-opus-4-5"));
        when(analysisRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        service.getOrGenerate(1L);

        verify(llmClient).complete(anyString(), userCaptor.capture(), anyInt());
        assertThat(userCaptor.getValue()).contains("Counter-Strike 2", "100,000", "HIGH", "<USER_DATA>");
    }

    // ===== fixtures =====

    private void wireContext() {
        when(gameQueryService.getDetail(1L)).thenReturn(sampleDetail());
        when(gameQueryService.getPlayerInsight(1L)).thenReturn(samplePlayer());
        when(economicsQueryService.getEconomicsInsight(1L)).thenReturn(sampleEconomics());
    }

    private static AnalysisJpaEntity freshEntity(long gameId, String content, Instant createdAt) {
        try {
            var ctor = AnalysisJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            var e = ctor.newInstance();
            e.setGameId(gameId);
            e.setKind(AnalysisKind.INSIGHT_BRIEF);
            e.setPromptVersion(PromptBuilder.version(com.gametrend.insight.domain.insight.Persona.DEFAULT));
            e.setContent(content);
            e.setPromptTokens(100);
            e.setCompletionTokens(50);
            e.setTotalTokens(150);
            e.setModel("claude-opus-4-5");
            e.setCreatedAt(createdAt);
            e.setExpiresAt(createdAt.plusSeconds(86400));
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static GameDetailItem sampleDetail() {
        return new GameDetailItem(1L, 730L, null, "Counter-Strike 2", null, "Valve", "Valve",
                LocalDate.of(2023, 9, 27), null, List.of("Action", "FPS"),
                100_000, 4.7,
                Instant.parse("2026-05-06T00:00:00Z"), Instant.parse("2026-05-06T01:00:00Z"));
    }

    private static PlayerInsight samplePlayer() {
        return new PlayerInsight(
                1L,
                new PlayerInsight.PlayerStats(100_000, 95_000, 100_000, 95.0),
                50_000,
                List.of(
                        new PlayerInsight.MentionByPlatform("YOUTUBE", 10_000, Instant.now()),
                        new PlayerInsight.MentionByPlatform("REDDIT", 2_500, Instant.now())),
                Instant.now());
    }

    private static EconomicsInsight sampleEconomics() {
        return new EconomicsInsight(
                1L,
                new EconomicsInsight.RevenueEstimate(
                        50_000_000L, 100_000_000L, 75_000_000L,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        800_000, 2_800_000),
                new EconomicsInsight.UnitEconomics(2.0, 0.125, null, null),
                Confidence.HIGH,
                Instant.now());
    }
}
