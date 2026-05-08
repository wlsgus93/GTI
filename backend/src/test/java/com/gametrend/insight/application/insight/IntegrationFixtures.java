package com.gametrend.insight.application.insight;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.PlayerInsight;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** InsightFlowIT 통합 테스트용 공유 fixture. */
final class IntegrationFixtures {

    private IntegrationFixtures() {}

    static GameDetailItem detail() {
        return new GameDetailItem(1L, 730L, null, "Counter-Strike 2", null, "Valve", "Valve",
                LocalDate.of(2023, 9, 27), null, List.of("Action", "FPS"),
                100_000, 4.7,
                Instant.parse("2026-05-06T00:00:00Z"), Instant.parse("2026-05-06T01:00:00Z"));
    }

    static PlayerInsight player() {
        return new PlayerInsight(
                1L,
                new PlayerInsight.PlayerStats(100_000, 95_000, 100_000, 95.0),
                50_000,
                List.of(
                        new PlayerInsight.MentionByPlatform("YOUTUBE", 10_000, Instant.now()),
                        new PlayerInsight.MentionByPlatform("REDDIT", 2_500, Instant.now())),
                Instant.now());
    }

    static EconomicsInsight economics() {
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
