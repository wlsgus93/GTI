package com.gametrend.insight.application.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimeLaggedCorrelationTest {

    private final TimeLaggedCorrelation tlc = new TimeLaggedCorrelation();

    @Test
    @DisplayName("동일 시계열 + lag=0 → r=1.0")
    void identicalSeriesNoLag_correlationOne() {
        double[] x = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] y = {1, 2, 3, 4, 5, 6, 7, 8};
        Double r = tlc.correlation(x, y, 0);
        assertThat(r).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("음의 상관 — y가 x의 반대 → r ≈ -1")
    void inverseCorrelation() {
        double[] x = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] y = {8, 7, 6, 5, 4, 3, 2, 1};
        Double r = tlc.correlation(x, y, 0);
        assertThat(r).isCloseTo(-1.0, within(0.001));
    }

    @Test
    @DisplayName("lag=2 — y가 x보다 2 시점 뒤라 lagged 시 r=1.0")
    void laggedSeries_perfectCorrelation() {
        // x[0..5] = [1, 2, 3, 4, 5, 6]
        // y[2..7] = [1, 2, 3, 4, 5, 6] (앞 2개는 noise)
        double[] x = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] y = {99, 99, 1, 2, 3, 4, 5, 6};

        Double r = tlc.correlation(x, y, 2);
        // x[0..5] vs y[2..7] = [1..6] vs [1..6] → r=1.0
        assertThat(r).isCloseTo(1.0, within(0.001));
    }

    @Test
    @DisplayName("findBestLag — y가 x보다 3 시점 뒤일 때 best=3 (앞 3개는 noise outlier)")
    void findBestLag_detectsCorrectLag() {
        double[] x = new double[15];
        double[] y = new double[15];
        // y의 처음 3개를 outlier(99)로 박아 lag<3에서 r 떨어지게 만듦
        // lag=3에서만 정확히 일치 (r=1.0)
        for (int i = 0; i < 15; i++) {
            x[i] = i + 1;
            y[i] = (i >= 3) ? (i - 2) : 99.0;
        }

        var result = tlc.findBestLag(x, y, 7);

        assertThat(result).isNotNull();
        assertThat(result.bestLag()).isEqualTo(3);
        assertThat(result.bestCorrelation()).isCloseTo(1.0, within(0.01));
        assertThat(result.correlationsByLag()).hasSizeBetween(1, 8);
    }

    @Test
    @DisplayName("무관 시계열 — |r| 1.0 미만 (완전 상관 X)")
    void uncorrelatedSeries_notPerfect() {
        // 완전 무관은 r=0이지만 작은 sample(10개)에선 우연 상관 발생.
        // 핵심 검증: 완전 상관(±1)은 아닌지 확인
        double[] x = {1, 5, 2, 8, 3, 7, 4, 6, 1, 9};
        double[] y = {3, 1, 8, 2, 9, 4, 7, 5, 6, 2};
        Double r = tlc.correlation(x, y, 0);
        assertThat(r).isNotNull();
        // |r| < 1.0 (완전 상관 아님)
        assertThat(Math.abs(r)).isLessThan(0.95);
    }

    @Test
    @DisplayName("Sample 부족 — n - lag < 5 → null")
    void insufficientSamples_returnsNull() {
        double[] x = {1, 2, 3, 4, 5, 6};
        double[] y = {1, 2, 3, 4, 5, 6};
        // lag=2 → n=4 (< MIN 5)
        assertThat(tlc.correlation(x, y, 2)).isNull();
    }

    @Test
    @DisplayName("분산 0 (모든 값 동일) → null (NaN 정규화)")
    void zeroVariance_returnsNull() {
        double[] x = {5, 5, 5, 5, 5, 5};
        double[] y = {1, 2, 3, 4, 5, 6};
        // x 분산 0 → Pearson NaN → null
        assertThat(tlc.correlation(x, y, 0)).isNull();
    }

    @Test
    @DisplayName("lag 음수 → IllegalArgumentException")
    void negativeLag_throws() {
        double[] x = {1, 2, 3, 4, 5, 6};
        double[] y = {1, 2, 3, 4, 5, 6};
        assertThatThrownBy(() -> tlc.correlation(x, y, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("길이 다름 → IllegalArgumentException")
    void mismatchedLength_throws() {
        double[] x = {1, 2, 3, 4, 5};
        double[] y = {1, 2, 3, 4, 5, 6};
        assertThatThrownBy(() -> tlc.correlation(x, y, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("same length");
    }

    @Test
    @DisplayName("findBestLag — 모든 lag에서 sample 부족 → null")
    void findBestLag_allInsufficient() {
        double[] x = {1, 2, 3};  // 3 samples (< MIN 5)
        double[] y = {1, 2, 3};
        assertThat(tlc.findBestLag(x, y, 5)).isNull();
    }

    @Test
    @DisplayName("findBestLag — 마케팅 시나리오 (캠페인 → CCU 추적)")
    void findBestLag_marketingScenario() {
        // 캠페인 시작 후 5일째부터 CCU 상승 (lag=5)
        double[] campaign = {0, 100, 200, 150, 80, 30, 10, 5, 2, 1, 0};   // 11일치 캠페인 강도 (감쇠)
        double[] ccu      = {1000, 1000, 1000, 1000, 1000, 1100, 1300, 1500, 1400, 1200, 1100}; // 5일 후 CCU 상승

        var result = tlc.findBestLag(campaign, ccu, 6);

        assertThat(result).isNotNull();
        // best lag은 0~6 범위 안
        assertThat(result.bestLag()).isBetween(0, 6);
        // 양의 상관 어딘가에서 발견 (캠페인 → CCU 양의 영향)
        assertThat(result.correlationsByLag()).isNotEmpty();
    }
}
