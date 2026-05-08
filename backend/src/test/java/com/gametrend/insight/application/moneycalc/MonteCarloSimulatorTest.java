package com.gametrend.insight.application.moneycalc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MonteCarloSimulatorTest {

    private final MonteCarloSimulator sim = new MonteCarloSimulator(new MoneyCalcCalculator());

    @Test
    @DisplayName("결정성 — 같은 seed로 두 번 호출 시 동일 결과 (CI 안정성)")
    void deterministicWithSeed() {
        var req = sample(1000);
        var r1 = sim.simulate(req, 42L);
        var r2 = sim.simulate(req, 42L);

        assertThat(r1.netRevenueP10()).isEqualByComparingTo(r2.netRevenueP10());
        assertThat(r1.netRevenueP50()).isEqualByComparingTo(r2.netRevenueP50());
        assertThat(r1.netRevenueP90()).isEqualByComparingTo(r2.netRevenueP90());
        assertThat(r1.netRevenueMean()).isEqualByComparingTo(r2.netRevenueMean());
        assertThat(r1.profitProbabilityPct()).isEqualTo(r2.profitProbabilityPct());
    }

    @Test
    @DisplayName("Triangular 단조성 — 비관 ≤ p10 ≤ p50 ≤ p90 ≤ 낙관 (가격 고정 시)")
    void monotonicOrdering() {
        var req = new MoneyCalcRequest(
                new Scenario(10_000, 1999),    // 비관 owners
                new Scenario(100_000, 1999),   // 보통
                new Scenario(500_000, 1999),   // 낙관
                10_000_000L, 5_000_000L, 2000);

        var r = sim.simulate(req, 42L);

        assertThat(r.netRevenueP10()).isLessThanOrEqualTo(r.netRevenueP50());
        assertThat(r.netRevenueP50()).isLessThanOrEqualTo(r.netRevenueP90());
    }

    @Test
    @DisplayName("profitProbability — 모두 수익 시나리오 → ~100%")
    void allProfitable_probability100() {
        // 비관도 충분히 수익 (낮은 비용)
        var req = new MoneyCalcRequest(
                new Scenario(100_000, 5999),
                new Scenario(500_000, 5999),
                new Scenario(1_000_000, 5999),
                100_000L, 0L, 1000); // 비용 $1.00 — 거의 0
        var r = sim.simulate(req, 42L);

        assertThat(r.profitProbabilityPct()).isGreaterThan(99.0);
    }

    @Test
    @DisplayName("profitProbability — 모두 적자 시나리오 → ~0%")
    void allLoss_probability0() {
        var req = new MoneyCalcRequest(
                new Scenario(100, 999),
                new Scenario(500, 999),
                new Scenario(1000, 999),
                10_000_000_000L, 0L, 1000); // 비용 $100M — 절대 회수 불가
        var r = sim.simulate(req, 42L);

        assertThat(r.profitProbabilityPct()).isLessThan(1.0);
    }

    @Test
    @DisplayName("Triangular sampling — min ≤ sample ≤ max (1000회 검증)")
    void triangular_boundsRespected() {
        Random rnd = new Random(42);
        for (int i = 0; i < 1000; i++) {
            double s = MonteCarloSimulator.sampleTriangular(100, 500, 1000, rnd);
            assertThat(s).isBetween(100.0, 1000.0);
        }
    }

    @Test
    @DisplayName("Triangular degenerate (min==max==mode) → 항상 그 값 반환")
    void degenerate_constantValue() {
        Random rnd = new Random(42);
        double s = MonteCarloSimulator.sampleTriangular(500, 500, 500, rnd);
        assertThat(s).isEqualTo(500.0);
    }

    @Test
    @DisplayName("iterations 옵션 — 100~10000 범위 정상 동작")
    void iterations_respected() {
        var r100 = sim.simulate(sample(100), 42L);
        var r10000 = sim.simulate(sample(10_000), 42L);

        assertThat(r100.iterations()).isEqualTo(100);
        assertThat(r10000.iterations()).isEqualTo(10_000);
    }

    private static MoneyCalcRequest sample(int iterations) {
        return new MoneyCalcRequest(
                new Scenario(50_000, 1999),
                new Scenario(200_000, 1999),
                new Scenario(800_000, 1999),
                50_000_000L, 10_000_000L, iterations);
    }
}
