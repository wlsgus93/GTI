package com.gametrend.insight.application.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EWMACalculatorTest {

    private final EWMACalculator ewma = new EWMACalculator();

    @Test
    @DisplayName("정상 계산 — α=0.3, [10,20,30] → EMA 평활")
    void typicalCase() {
        double[] values = {10, 20, 30};
        double[] result = ewma.calculate(values, 0.3);

        // EMA_0 = 10 (= x_0)
        assertThat(result[0]).isEqualTo(10.0);
        // EMA_1 = 0.3 × 20 + 0.7 × 10 = 6 + 7 = 13
        assertThat(result[1]).isCloseTo(13.0, org.assertj.core.api.Assertions.within(0.001));
        // EMA_2 = 0.3 × 30 + 0.7 × 13 = 9 + 9.1 = 18.1
        assertThat(result[2]).isCloseTo(18.1, org.assertj.core.api.Assertions.within(0.01));
    }

    @Test
    @DisplayName("α 경계값 — 0 또는 1 → IllegalArgumentException")
    void invalidAlpha() {
        double[] v = {1, 2, 3};
        assertThatThrownBy(() -> ewma.calculate(v, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ewma.calculate(v, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ewma.calculate(v, -0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ewma.calculate(v, 1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Momentum — 7일 전 대비 현재 EWMA 변화율")
    void momentum() {
        // 점진적 상승: 100 → 110 → 120 → ... → 170 (8 시점)
        double[] values = {100, 110, 120, 130, 140, 150, 160, 170};
        Double m = ewma.momentum(values, 0.3, 7);

        // 7시점 전(=index 0) EMA=100, 현재 EMA는 170 미만이지만 평균 70+α
        // momentum = ((current - past) / past) × 100 → 양수
        assertThat(m).isNotNull().isPositive();
    }

    @Test
    @DisplayName("Momentum — lookback 부족 시 null (sample 부족)")
    void momentum_insufficientLookback() {
        double[] values = {1, 2};
        assertThat(ewma.momentum(values, 0.3, 5)).isNull();
    }

    @Test
    @DisplayName("current — 마지막 EWMA 값 = calculate(...).last")
    void currentMatchesLast() {
        double[] values = {10, 20, 30, 40};
        double current = ewma.current(values, 0.3);
        double[] all = ewma.calculate(values, 0.3);
        assertThat(current).isEqualTo(all[all.length - 1]);
    }

    @Test
    @DisplayName("빈 배열 / null — 빈 결과")
    void edgeCases() {
        assertThat(ewma.calculate(new double[0], 0.3)).isEmpty();
        assertThat(ewma.calculate(null, 0.3)).isEmpty();
        assertThat(ewma.current(new double[0], 0.3)).isEqualTo(0.0);
    }
}
