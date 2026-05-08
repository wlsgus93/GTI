package com.gametrend.insight.application.economics;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RevenueEstimatorTest {

    private final RevenueEstimator estimator = new RevenueEstimator();

    @Nested
    @DisplayName("estimate")
    class Estimate {

        @Test
        @DisplayName("정상 케이스 — Elden Ring 기준 (20M~30M, $59.99, ccu 200K)")
        void typicalCase() {
            var r = estimator.estimate(20_000_000L, 30_000_000L, 5999L, 200_000);

            assertThat(r).isNotNull();
            assertThat(r.ownersMid()).isEqualTo(25_000_000L);
            assertThat(r.priceUsd()).isEqualByComparingTo("59.99");
            // gross = 25M × 59.99 = 1,499,750,000
            assertThat(r.grossLifetimeRevenue()).isEqualByComparingTo("1499750000.00");
            // afterRefund = gross × 0.95 = 1,424,762,500
            assertThat(r.afterRefundRevenue()).isEqualByComparingTo("1424762500.00");
            // developerNet = afterRefund × 0.70 = 997,333,750
            assertThat(r.developerNet()).isEqualByComparingTo("997333750.00");
            // DAU = 200K × 8 = 1.6M, MAU = 1.6M × 3.5 = 5.6M
            assertThat(r.estimatedDau()).isEqualTo(1_600_000);
            assertThat(r.estimatedMau()).isEqualTo(5_600_000);
        }

        @Test
        @DisplayName("F2P (price=0) — gross/afterRefund/developerNet 모두 0")
        void f2pZeroPrice() {
            var r = estimator.estimate(50_000_000L, 100_000_000L, 0L, 1_000_000);

            assertThat(r).isNotNull();
            assertThat(r.priceUsd()).isEqualByComparingTo("0.00");
            assertThat(r.grossLifetimeRevenue()).isEqualByComparingTo("0.00");
            assertThat(r.afterRefundRevenue()).isEqualByComparingTo("0.00");
            assertThat(r.developerNet()).isEqualByComparingTo("0.00");
            assertThat(r.estimatedDau()).isEqualTo(8_000_000);
        }

        @Test
        @DisplayName("price null — gross/afterRefund/developerNet null, owners/dau는 채워짐")
        void priceNull_partialResult() {
            var r = estimator.estimate(10_000_000L, 15_000_000L, null, 50_000);

            assertThat(r).isNotNull();
            assertThat(r.ownersMid()).isEqualTo(12_500_000L);
            assertThat(r.priceUsd()).isNull();
            assertThat(r.grossLifetimeRevenue()).isNull();
            assertThat(r.afterRefundRevenue()).isNull();
            assertThat(r.developerNet()).isNull();
            assertThat(r.estimatedDau()).isEqualTo(400_000);
            assertThat(r.estimatedMau()).isEqualTo(1_400_000);
        }

        @Test
        @DisplayName("owners null → estimate 자체 null")
        void ownersNull() {
            assertThat(estimator.estimate(null, 30_000_000L, 5999L, 200_000)).isNull();
            assertThat(estimator.estimate(20_000_000L, null, 5999L, 200_000)).isNull();
        }

        @Test
        @DisplayName("ccuPeak null → DAU/MAU null, 매출은 정상")
        void ccuPeakNull_dauNull() {
            var r = estimator.estimate(1_000_000L, 2_000_000L, 1499L, null);

            assertThat(r).isNotNull();
            assertThat(r.estimatedDau()).isNull();
            assertThat(r.estimatedMau()).isNull();
            assertThat(r.developerNet()).isNotNull();
        }

        @Test
        @DisplayName("계산 체인 검증 — 각 단계 비율 정확")
        void chainPercentages() {
            // 1M owners, $10
            var r = estimator.estimate(1_000_000L, 1_000_000L, 1000L, null);
            assertThat(r.grossLifetimeRevenue()).isEqualByComparingTo("10000000.00");
            // afterRefund = 10M × 0.95 = 9.5M
            assertThat(r.afterRefundRevenue()).isEqualByComparingTo("9500000.00");
            // developerNet = 9.5M × 0.70 = 6.65M
            assertThat(r.developerNet()).isEqualByComparingTo(new BigDecimal("6650000.00"));
        }
    }

    @Nested
    @DisplayName("assessConfidence")
    class AssessConfidence {

        @Test
        @DisplayName("HIGH — 좁은 owners 범위 + 가격 + fresh")
        void high() {
            // 110~130 (mid 120, width 20, ratio 0.166 < 0.30)
            assertThat(estimator.assessConfidence(110_000L, 130_000L, 1999L, true))
                    .isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("MEDIUM — fresh=false라도 폭 < 60%")
        void medium_notFresh() {
            assertThat(estimator.assessConfidence(110_000L, 130_000L, 1999L, false))
                    .isEqualTo(Confidence.MEDIUM);
        }

        @Test
        @DisplayName("MEDIUM — 폭 30~60% 사이")
        void medium_widerRange() {
            // 100~150 (mid 125, width 50, ratio 0.40)
            assertThat(estimator.assessConfidence(100_000L, 150_000L, 1999L, true))
                    .isEqualTo(Confidence.MEDIUM);
        }

        @Test
        @DisplayName("LOW — 폭 ≥ 60% (SteamSpy 큰 게임 흔함)")
        void low_wideRange() {
            // 50M~100M (mid 75M, width 50M, ratio 0.667)
            assertThat(estimator.assessConfidence(50_000_000L, 100_000_000L, 0L, true))
                    .isEqualTo(Confidence.LOW);
        }

        @Test
        @DisplayName("LOW — 가격 null")
        void low_priceNull() {
            assertThat(estimator.assessConfidence(100L, 110L, null, true))
                    .isEqualTo(Confidence.LOW);
        }

        @Test
        @DisplayName("LOW — owners null")
        void low_ownersNull() {
            assertThat(estimator.assessConfidence(null, 110L, 1999L, true))
                    .isEqualTo(Confidence.LOW);
        }

        @Test
        @DisplayName("LOW — ownersMid 0 (DivisionByZero 가드)")
        void low_zeroOwners() {
            assertThat(estimator.assessConfidence(0L, 0L, 1999L, true))
                    .isEqualTo(Confidence.LOW);
        }
    }
}
