package com.gametrend.insight.application.moneycalc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 민감도 분석 — one-at-a-time (OAT).
 *
 * <p>방법: 모든 변수를 보통값(realistic)으로 고정하고, 한 변수만 비관/낙관으로 변동.
 * net 차이를 realistic net 대비 비율로 계산. 비율 큰 변수 = 영향 큰 변수.
 *
 * <p>한계: 변수 간 상호작용 무시 (Sobol 같은 글로벌 sensitivity는 W4 후속).
 */
@Component
public class SensitivityAnalyzer {

    private final MoneyCalcCalculator calculator;

    public SensitivityAnalyzer(MoneyCalcCalculator calculator) {
        this.calculator = calculator;
    }

    public List<MoneyCalcResult.SensitivityImpact> analyze(MoneyCalcRequest req) {
        long totalCost = req.totalCostCents();
        BigDecimal realisticNet = calculator.calculate(req.realistic(), totalCost).developerNetUsd();

        // 1) owners만 변동
        var ownersPessNet = calculator.calculate(
                new Scenario(req.pessimistic().owners(), req.realistic().priceCents()), totalCost).developerNetUsd();
        var ownersOptNet = calculator.calculate(
                new Scenario(req.optimistic().owners(), req.realistic().priceCents()), totalCost).developerNetUsd();
        Double ownersImpact = impactRatio(ownersPessNet, ownersOptNet, realisticNet);

        // 2) priceCents만 변동
        var pricePessNet = calculator.calculate(
                new Scenario(req.realistic().owners(), req.pessimistic().priceCents()), totalCost).developerNetUsd();
        var priceOptNet = calculator.calculate(
                new Scenario(req.realistic().owners(), req.optimistic().priceCents()), totalCost).developerNetUsd();
        Double priceImpact = impactRatio(pricePessNet, priceOptNet, realisticNet);

        // impact desc 정렬
        List<MoneyCalcResult.SensitivityImpact> impacts = new java.util.ArrayList<>();
        impacts.add(new MoneyCalcResult.SensitivityImpact("owners", ownersPessNet, ownersOptNet, ownersImpact));
        impacts.add(new MoneyCalcResult.SensitivityImpact("priceCents", pricePessNet, priceOptNet, priceImpact));
        impacts.sort((a, b) -> {
            Double ai = a.impactRatio() == null ? 0.0 : a.impactRatio();
            Double bi = b.impactRatio() == null ? 0.0 : b.impactRatio();
            return Double.compare(bi, ai);
        });
        return impacts;
    }

    private static Double impactRatio(BigDecimal pessNet, BigDecimal optNet, BigDecimal realNet) {
        if (realNet.compareTo(BigDecimal.ZERO) == 0) return null;
        BigDecimal diff = optNet.subtract(pessNet).abs();
        BigDecimal ratio = diff.divide(realNet.abs(), 4, RoundingMode.HALF_UP);
        return ratio.doubleValue();
    }
}
