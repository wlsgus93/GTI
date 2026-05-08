package com.gametrend.insight.application.moneycalc;

import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * P5 MoneyCalc — Pretotyping 의사결정 시뮬레이터.
 *
 * <p>흐름:
 * <ol>
 *   <li>입력 검증 (비즈니스 규칙)
 *   <li>3 결정론적 시나리오 계산 (gross/net/profit/ROI/BEP)
 *   <li>Monte Carlo (Triangular sampling) — p10/p50/p90/mean/stdDev + profit %
 *   <li>OAT 민감도 분석 — owners vs priceCents 영향 비교
 * </ol>
 */
@Service
public class MoneyCalcService {

    /** 재현 가능한 결과를 위한 기본 seed. 비결정성 원하면 호출자가 nanoTime 사용. */
    public static final long DEFAULT_SEED = 42L;

    private final MoneyCalcCalculator calculator;
    private final MonteCarloSimulator simulator;
    private final SensitivityAnalyzer sensitivity;

    public MoneyCalcService(
            MoneyCalcCalculator calculator,
            MonteCarloSimulator simulator,
            SensitivityAnalyzer sensitivity) {
        this.calculator = calculator;
        this.simulator = simulator;
        this.sensitivity = sensitivity;
    }

    public MoneyCalcResult simulate(MoneyCalcRequest req) {
        return simulate(req, DEFAULT_SEED);
    }

    public MoneyCalcResult simulate(MoneyCalcRequest req, long seed) {
        req.validateBusiness();

        long totalCost = req.totalCostCents();
        var pess = calculator.calculate(req.pessimistic(), totalCost);
        var real = calculator.calculate(req.realistic(), totalCost);
        var opt = calculator.calculate(req.optimistic(), totalCost);

        var monte = simulator.simulate(req, seed);
        var impacts = sensitivity.analyze(req);

        var assumptions = new MoneyCalcResult.Assumptions(
                MoneyCalcCalculator.REFUND_RATE,
                MoneyCalcCalculator.STEAM_CUT,
                0,  // 0 = triangular
                seed);

        return new MoneyCalcResult(pess, real, opt, monte, impacts, assumptions, Instant.now());
    }
}
