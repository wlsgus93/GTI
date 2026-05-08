package com.gametrend.insight.application.moneycalc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * 결정론적 시나리오 계산 — 순수 함수 (Spring 의존 X 가능, @Component는 DI 편의).
 *
 * <p>RevenueEstimator (W2 D4)와 동일 모델이지만 비용/ROI/BEP까지 확장.
 */
@Component
public class MoneyCalcCalculator {

    public static final double REFUND_RATE = 0.05;
    public static final double STEAM_CUT = 0.30;

    /**
     * 단일 시나리오 → 매출 + 비용 + ROI + BEP.
     *
     * @param scenario      owners + priceCents
     * @param totalCostCents 개발 + 마케팅 비용 (cents)
     */
    public MoneyCalcResult.ScenarioOutput calculate(Scenario scenario, long totalCostCents) {
        BigDecimal priceUsd = BigDecimal.valueOf(scenario.priceCents()).movePointLeft(2);
        BigDecimal owners = BigDecimal.valueOf(scenario.owners());
        BigDecimal totalCostUsd = BigDecimal.valueOf(totalCostCents).movePointLeft(2);

        BigDecimal gross = priceUsd.multiply(owners).setScale(2, RoundingMode.HALF_UP);
        BigDecimal afterRefund = gross.multiply(BigDecimal.valueOf(1.0 - REFUND_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal developerNet = afterRefund.multiply(BigDecimal.valueOf(1.0 - STEAM_CUT))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal profit = developerNet.subtract(totalCostUsd).setScale(2, RoundingMode.HALF_UP);

        Double roiPct = null;
        if (totalCostCents > 0) {
            roiPct = profit.divide(totalCostUsd, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        // BEP: 단위당 net = price × 0.95 × 0.70. totalCost / 단위net = 손익분기 단위
        Long bepUnits = null;
        if (priceUsd.compareTo(BigDecimal.ZERO) > 0 && totalCostCents > 0) {
            BigDecimal perUnitNet = priceUsd
                    .multiply(BigDecimal.valueOf(1.0 - REFUND_RATE))
                    .multiply(BigDecimal.valueOf(1.0 - STEAM_CUT));
            bepUnits = totalCostUsd.divide(perUnitNet, 0, RoundingMode.CEILING).longValueExact();
        }

        return new MoneyCalcResult.ScenarioOutput(
                gross, afterRefund, developerNet, totalCostUsd, profit, roiPct, bepUnits);
    }

    /**
     * Monte Carlo iteration의 한 번 — net 매출만 빠르게 (비용 차감 안 함).
     * Hot loop이므로 BigDecimal 회피하고 double로.
     */
    public double iterateNetUsd(double owners, double priceUsd) {
        return owners * priceUsd * (1.0 - REFUND_RATE) * (1.0 - STEAM_CUT);
    }
}
