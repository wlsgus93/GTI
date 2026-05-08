package com.gametrend.insight.application.moneycalc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * P5 MoneyCalc 정량 측정 (W4 D3 — STAR-D #5 입력 데이터).
 *
 * <p>측정 대상:
 * <ul>
 *   <li>Monte Carlo wall-clock — iteration 수에 따른 latency
 *   <li>Triangular sampling 분포 정확성 — p10/p50/p90 시나리오별 비교
 *   <li>profit probability 시나리오 결정성에 따른 변화
 *   <li>민감도 — owners vs priceCents 영향 비교
 * </ul>
 *
 * <p>출력: System.out.printf — STAR-D 작성 시 직접 인용.
 */
class MoneyCalcBenchmark {

    private final MoneyCalcService service = new MoneyCalcService(
            new MoneyCalcCalculator(),
            new MonteCarloSimulator(new MoneyCalcCalculator()),
            new SensitivityAnalyzer(new MoneyCalcCalculator()));

    @Test
    @DisplayName("[BENCH] Monte Carlo wall-clock — 100 / 1000 / 10000 iterations")
    void wallClockByIterations() {
        var pess = new Scenario(50_000, 1999);
        var real = new Scenario(200_000, 1999);
        var opt = new Scenario(800_000, 1999);
        long devCost = 50_000_000L;
        long mktCost = 10_000_000L;

        // Warm-up (JIT)
        for (int i = 0; i < 3; i++) {
            service.simulate(new MoneyCalcRequest(pess, real, opt, devCost, mktCost, 1000), 42L);
        }

        for (int iter : new int[] {100, 1000, 10000}) {
            var req = new MoneyCalcRequest(pess, real, opt, devCost, mktCost, iter);
            long t0 = System.nanoTime();
            var result = service.simulate(req, 42L);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            System.out.printf(
                    "[Bench] iter=%5d  wall=%4dms  netP10=%-12s  netP50=%-12s  netP90=%-12s  profitProb=%.1f%%%n",
                    iter, ms,
                    result.monteCarlo().netRevenueP10(),
                    result.monteCarlo().netRevenueP50(),
                    result.monteCarlo().netRevenueP90(),
                    result.monteCarlo().profitProbabilityPct());
        }
    }

    @Test
    @DisplayName("[BENCH] 시나리오별 profit probability — 3 게임 케이스 비교")
    void scenarioComparison() {
        // 1. Indie (저비용, 낮은 owners 추정 폭) — 보수적
        bench("Indie ($14.99, $50K dev)",
                new Scenario(20_000, 1499), new Scenario(80_000, 1499), new Scenario(200_000, 1499),
                5_000_000L, 1_000_000L);
        // 2. Mid-AAA ($59.99, $5M dev)
        bench("Mid-AAA ($59.99, $5M dev)",
                new Scenario(50_000, 5999), new Scenario(300_000, 5999), new Scenario(1_500_000, 5999),
                500_000_000L, 200_000_000L);
        // 3. 고위험 (낙관 매우 큼, 비관 매우 작음 → profit prob 중간)
        bench("High-Variance ($29.99)",
                new Scenario(5_000, 2999), new Scenario(150_000, 2999), new Scenario(2_000_000, 2999),
                30_000_000L, 10_000_000L);
    }

    @Test
    @DisplayName("[BENCH] 민감도 — owners 변동폭 vs price 변동폭")
    void sensitivityComparison() {
        // 1. owners 변동만 큼 (price 일정)
        var ownersVary = new MoneyCalcRequest(
                new Scenario(10_000, 1999), new Scenario(100_000, 1999), new Scenario(1_000_000, 1999),
                10_000_000L, 0L, 1000);
        var r1 = service.simulate(ownersVary, 42L);
        System.out.printf("[Bench Sensitivity] owners 100×범위, price 일정: %s%n", r1.sensitivity());

        // 2. price 변동만 큼 (owners 일정)
        var priceVary = new MoneyCalcRequest(
                new Scenario(100_000, 999), new Scenario(100_000, 2999), new Scenario(100_000, 5999),
                10_000_000L, 0L, 1000);
        var r2 = service.simulate(priceVary, 42L);
        System.out.printf("[Bench Sensitivity] owners 일정, price 6×범위: %s%n", r2.sensitivity());

        // 3. 둘 다 변동 (현실)
        var both = new MoneyCalcRequest(
                new Scenario(50_000, 1499), new Scenario(200_000, 1999), new Scenario(800_000, 2999),
                10_000_000L, 0L, 1000);
        var r3 = service.simulate(both, 42L);
        System.out.printf("[Bench Sensitivity] 둘 다 변동: %s%n", r3.sensitivity());
    }

    private void bench(String label, Scenario p, Scenario r, Scenario o, long dev, long mkt) {
        var req = new MoneyCalcRequest(p, r, o, dev, mkt, 1000);
        long t0 = System.nanoTime();
        var result = service.simulate(req, 42L);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        System.out.printf(
                "[Bench] %-32s  wall=%4dms  realisticROI=%6.1f%%  profitProb=%5.1f%%  netP50=%s%n",
                label, ms,
                result.realistic().roiPct() == null ? 0.0 : result.realistic().roiPct(),
                result.monteCarlo().profitProbabilityPct(),
                result.monteCarlo().netRevenueP50());
    }
}
