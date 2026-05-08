package com.gametrend.insight.application.economics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UnitEconomicsCalculatorTest {

    private final UnitEconomicsCalculator calculator = new UnitEconomicsCalculator();

    @Test
    @DisplayName("정상 케이스 — 모든 분모 양수")
    void allPositive() {
        var u = calculator.calculate(100_000, 50_000, 12_500, 5999L, 95_234);

        assertThat(u.viewToPlayRatio()).isEqualTo(2.0); // 100K/50K
        assertThat(u.mentionToPlayRatio()).isEqualTo(0.125); // 12.5K/100K
        assertThat(u.priceEfficiency()).isEqualTo(1666.944); // 100K/59.99 → 1666.9445 rounded
        // 59.99 / 95234 = 0.0006 (4자리 round-half-up)
        assertThat(u.reviewCostPerPositive()).isEqualByComparingTo("0.0006");
    }

    @Test
    @DisplayName("twitchViewers 0 → viewToPlay null")
    void zeroViewers_viewRatioNull() {
        var u = calculator.calculate(100_000, 0, 1000, 1499L, 5000);

        assertThat(u.viewToPlayRatio()).isNull();
        assertThat(u.mentionToPlayRatio()).isNotNull();
        assertThat(u.priceEfficiency()).isNotNull();
    }

    @Test
    @DisplayName("F2P (price=0) → priceEfficiency, reviewCost null")
    void f2p_priceMetricsNull() {
        var u = calculator.calculate(100_000, 50_000, 12_500, 0L, 95_234);

        assertThat(u.viewToPlayRatio()).isEqualTo(2.0);
        assertThat(u.mentionToPlayRatio()).isEqualTo(0.125);
        assertThat(u.priceEfficiency()).isNull();
        assertThat(u.reviewCostPerPositive()).isNull();
    }

    @Test
    @DisplayName("리뷰 0 → reviewCost null, priceEfficiency는 정상")
    void zeroReviews_reviewCostNull() {
        var u = calculator.calculate(100_000, null, null, 5999L, 0);

        assertThat(u.priceEfficiency()).isNotNull();
        assertThat(u.reviewCostPerPositive()).isNull();
    }

    @Test
    @DisplayName("CCU null → 모든 ratio null")
    void ccuNull_allRatiosNull() {
        var u = calculator.calculate(null, 50_000, 1000, 5999L, 95_234);

        assertThat(u.viewToPlayRatio()).isNull();
        assertThat(u.mentionToPlayRatio()).isNull();
        assertThat(u.priceEfficiency()).isNull();
        assertThat(u.reviewCostPerPositive()).isNotNull(); // priceUsd / reviews만 의존
    }

    @Test
    @DisplayName("mentions null → mentionToPlay만 null")
    void mentionsNull() {
        var u = calculator.calculate(100_000, 50_000, null, 1499L, 1000);

        assertThat(u.mentionToPlayRatio()).isNull();
        assertThat(u.viewToPlayRatio()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("CCU 0 → mentionToPlay null (DivisionByZero 가드), viewToPlay 0")
    void ccuZero_guards() {
        var u = calculator.calculate(0, 50_000, 1000, 1499L, 100);

        assertThat(u.viewToPlayRatio()).isEqualTo(0.0); // 0/50K = 0
        assertThat(u.mentionToPlayRatio()).isNull(); // 1000/0 → guard
        assertThat(u.priceEfficiency()).isEqualTo(0.0); // 0/14.99
    }

    @Test
    @DisplayName("모든 입력 null → 모든 출력 null")
    void allNull() {
        var u = calculator.calculate(null, null, null, null, null);

        assertThat(u.viewToPlayRatio()).isNull();
        assertThat(u.mentionToPlayRatio()).isNull();
        assertThat(u.priceEfficiency()).isNull();
        assertThat(u.reviewCostPerPositive()).isNull();
    }
}
