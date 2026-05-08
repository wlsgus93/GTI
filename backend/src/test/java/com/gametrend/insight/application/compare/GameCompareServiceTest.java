package com.gametrend.insight.application.compare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.economics.EconomicsQueryService;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.game.GameQueryService;
import com.gametrend.insight.application.game.PlayerInsight;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GameCompareServiceTest {

    @Mock GameQueryService gameQueryService;
    @Mock EconomicsQueryService economicsQueryService;

    private GameCompareService service;

    @BeforeEach
    void setUp() {
        service = new GameCompareService(gameQueryService, economicsQueryService);
    }

    @Test
    @DisplayName("3 게임 비교 — 모두 정상 → items 3개, missing 0, 요청 순서 유지")
    void threeGames_allOk() {
        wireGame(1L, "Counter-Strike 2", 1_100_000);
        wireGame(2L, "Dota 2", 700_000);
        wireGame(3L, "ELDEN RING", 200_000);

        var result = service.compare(List.of(1L, 2L, 3L));

        assertThat(result.items()).hasSize(3);
        assertThat(result.missingGameIds()).isEmpty();
        // 요청 순서 유지
        assertThat(result.items()).extracting(CompareItem::gameId).containsExactly(1L, 2L, 3L);
        assertThat(result.wallClockMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("일부 미존재 → 부분 결과 + missingGameIds 표시 (graceful)")
    void partialMissing_gracefulDegradation() {
        wireGame(1L, "CS2", 1_000_000);
        when(gameQueryService.getDetail(eq(99L))).thenThrow(new GameNotFoundException(99L));
        wireGame(2L, "Dota2", 700_000);

        var result = service.compare(List.of(1L, 99L, 2L));

        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).extracting(CompareItem::gameId).containsExactly(1L, 2L);
        assertThat(result.missingGameIds()).containsExactly(99L);
    }

    @Test
    @DisplayName("ID 1개만 → IllegalArgumentException (최소 2개)")
    void tooFewIds_throws() {
        assertThatThrownBy(() -> service.compare(List.of(1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2");
    }

    @Test
    @DisplayName("ID 6개 → IllegalArgumentException (최대 5개, DoS 방어)")
    void tooManyIds_throws() {
        assertThatThrownBy(() -> service.compare(List.of(1L, 2L, 3L, 4L, 5L, 6L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("up to 5");
    }

    @Test
    @DisplayName("중복 ID dedupe — [1,1,2] → 2개로 처리")
    void duplicateIds_deduped() {
        wireGame(1L, "CS2", 1_000_000);
        wireGame(2L, "Dota2", 700_000);

        var result = service.compare(List.of(1L, 1L, 2L));

        assertThat(result.items()).hasSize(2);
        assertThat(result.items()).extracting(CompareItem::gameId).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("중복으로 dedupe 후 1개 남으면 → 422 (최소 2 distinct)")
    void duplicateReducedToOne_throws() {
        assertThatThrownBy(() -> service.compare(List.of(1L, 1L, 1L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2");
    }

    @Test
    @DisplayName("CompareItem 매핑 — economics + player + detail 모두 흡수")
    void mapping_allFieldsAbsorbed() {
        wireGame(1L, "CS2", 1_100_000);
        wireGame(2L, "Dota2", 700_000);

        var result = service.compare(List.of(1L, 2L));

        var first = result.items().get(0);
        assertThat(first.name()).isEqualTo("CS2");
        assertThat(first.steamAppId()).isEqualTo(730L);
        assertThat(first.latestCcu()).isEqualTo(1_100_000);
        assertThat(first.ccuDeltaPct()).isEqualTo(4.7);
        assertThat(first.twitchViewers()).isEqualTo(50_000);
        assertThat(first.totalMentions()).isEqualTo(12_500); // 10K + 2.5K
        assertThat(first.reviewScorePercent()).isEqualTo(95.0);
        assertThat(first.ownersMid()).isEqualTo(75_000_000L);
        assertThat(first.priceUsd()).isEqualByComparingTo("0.00");
        assertThat(first.confidence()).isEqualTo("HIGH");
        assertThat(first.genres()).containsExactly("Action", "FPS");
    }

    private void wireGame(long id, String name, int ccu) {
        when(gameQueryService.getDetail(id)).thenReturn(detail(id, name, ccu));
        when(gameQueryService.getPlayerInsight(id)).thenReturn(player(id));
        when(economicsQueryService.getEconomicsInsight(id)).thenReturn(economics(id));
    }

    private static GameDetailItem detail(long id, String name, int ccu) {
        return new GameDetailItem(id, 730L, null, name, null, "Valve", "Valve",
                LocalDate.of(2023, 9, 27), null, List.of("Action", "FPS"),
                ccu, 4.7,
                Instant.parse("2026-05-06T00:00:00Z"), Instant.parse("2026-05-06T01:00:00Z"));
    }

    private static PlayerInsight player(long id) {
        return new PlayerInsight(
                id,
                new PlayerInsight.PlayerStats(100_000, 95_000, 100_000, 95.0),
                50_000,
                List.of(
                        new PlayerInsight.MentionByPlatform("YOUTUBE", 10_000, Instant.now()),
                        new PlayerInsight.MentionByPlatform("REDDIT", 2_500, Instant.now())),
                Instant.now());
    }

    private static EconomicsInsight economics(long id) {
        return new EconomicsInsight(
                id,
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
