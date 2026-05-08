package com.gametrend.insight.application.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrendScoreCalculatorTest {

    private final TrendScoreCalculator calc = new TrendScoreCalculator();

    @Test
    @DisplayName("v1 정규화: ccu == maxCcu → score 100")
    void calculate_atMax_returns100() {
        assertThat(calc.calculate(1_000_000L, 1_000_000L)).isCloseTo(100.0, within(0.5));
    }

    @Test
    @DisplayName("ccu == 0 → score 0")
    void calculate_zero_returnsZero() {
        assertThat(calc.calculate(0L, 1_000_000L)).isEqualTo(0.0);
    }

    @Test
    @DisplayName("log 스케일: ccu/maxCcu = 0.5 일 때도 score는 50보다 큰 영역 (압축 X)")
    void calculate_logScale_compressed() {
        // ccu = 100k, maxCcu = 1M. log10(100001)/log10(1000001) ≈ 5/6 ≈ 83
        double score = calc.calculate(100_000L, 1_000_000L);
        assertThat(score).isGreaterThan(80.0).isLessThan(90.0);
    }

    @Test
    @DisplayName("deltaPct: 1000 → 1100 = +10%")
    void deltaPct_positive() {
        assertThat(calc.calculateDeltaPct(1100, 1000)).isEqualTo(10.0);
    }

    @Test
    @DisplayName("deltaPct: 1000 → 800 = -20%")
    void deltaPct_negative() {
        assertThat(calc.calculateDeltaPct(800, 1000)).isEqualTo(-20.0);
    }

    @Test
    @DisplayName("deltaPct: previous=0 → null (DivisionByZero 회피)")
    void deltaPct_previousZero_returnsNull() {
        assertThat(calc.calculateDeltaPct(100, 0)).isNull();
    }
}
