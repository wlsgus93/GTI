package com.gametrend.insight.application.stats;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ZScoreNormalizerTest {

    private final ZScoreNormalizer norm = new ZScoreNormalizer();

    @Test
    @DisplayName("정상 케이스 — [10, 20, 30, 40, 50] → mean=30, σ≈14.14")
    void typicalCase() {
        double[] values = {10, 20, 30, 40, 50};
        var result = norm.normalizeWithStats(values);

        assertThat(result.mean()).isEqualTo(30.0);
        assertThat(result.stdDev()).isCloseTo(14.142, org.assertj.core.api.Assertions.within(0.001));
        // z-score 검증: (10-30)/14.142 ≈ -1.414
        assertThat(result.zScores()[0]).isCloseTo(-1.414, org.assertj.core.api.Assertions.within(0.001));
        assertThat(result.zScores()[2]).isEqualTo(0.0); // mean과 동일
        assertThat(result.zScores()[4]).isCloseTo(1.414, org.assertj.core.api.Assertions.within(0.001));
    }

    @Test
    @DisplayName("stdDev=0 가드 — 모든 값 동일 시 z-score 모두 0")
    void zeroStdDev_returnsZero() {
        double[] values = {100, 100, 100, 100};
        var result = norm.normalizeWithStats(values);

        assertThat(result.stdDev()).isEqualTo(0.0);
        for (double z : result.zScores()) {
            assertThat(z).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("단일 값 정규화 — mean/stdDev 명시")
    void singleValue() {
        // (50 - 30) / 10 = 2.0
        assertThat(norm.normalize(50, 30, 10)).isEqualTo(2.0);
        // (10 - 30) / 10 = -2.0
        assertThat(norm.normalize(10, 30, 10)).isEqualTo(-2.0);
    }

    @Test
    @DisplayName("음수 값 정규화 — z-score는 음수 가능")
    void negativeValues() {
        double[] values = {-10, 0, 10};
        var result = norm.normalizeWithStats(values);
        assertThat(result.mean()).isEqualTo(0.0);
        // (-10 - 0) / σ → 음수
        assertThat(result.zScores()[0]).isLessThan(0.0);
        assertThat(result.zScores()[1]).isEqualTo(0.0);
        assertThat(result.zScores()[2]).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("빈 배열 — 빈 결과 반환")
    void emptyArray() {
        var result = norm.normalizeWithStats(new double[0]);
        assertThat(result.zScores()).isEmpty();
        assertThat(result.mean()).isEqualTo(0.0);
        assertThat(result.stdDev()).isEqualTo(0.0);
    }
}
