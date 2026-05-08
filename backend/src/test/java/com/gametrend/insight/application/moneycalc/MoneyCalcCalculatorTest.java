package com.gametrend.insight.application.moneycalc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyCalcCalculatorTest {

    private final MoneyCalcCalculator calc = new MoneyCalcCalculator();

    @Test
    @DisplayName("정상 시나리오 — owners 100K, $19.99, 비용 $500K → 수치 정확")
    void typicalCase() {
        // owners=100,000, price=$19.99
        // gross = 100K × 19.99 = 1,999,000
        // afterRefund = 1,999,000 × 0.95 = 1,899,050
        // net = 1,899,050 × 0.70 = 1,329,335
        // totalCost = $500,000
        // profit = 829,335
        // roi = 829,335 / 500,000 = 1.65867 → 165.87%
        // BEP: per-unit net = 19.99 × 0.95 × 0.70 = 13.29335
        // 500,000 / 13.29335 ≈ 37,612.79 → ceiling 37,613
        var out = calc.calculate(new Scenario(100_000, 1999), 50_000_000);

        assertThat(out.grossRevenueUsd()).isEqualByComparingTo("1999000.00");
        assertThat(out.afterRefundUsd()).isEqualByComparingTo("1899050.00");
        assertThat(out.developerNetUsd()).isEqualByComparingTo("1329335.00");
        assertThat(out.totalCostUsd()).isEqualByComparingTo("500000.00");
        assertThat(out.profitUsd()).isEqualByComparingTo("829335.00");
        assertThat(out.roiPct()).isEqualTo(165.87);
        assertThat(out.bepUnits()).isEqualTo(37_613L);
    }

    @Test
    @DisplayName("totalCost 0 → roi/bep null (DivisionByZero 가드)")
    void zeroCost() {
        var out = calc.calculate(new Scenario(100_000, 1999), 0);
        assertThat(out.profitUsd()).isEqualByComparingTo("1329335.00");
        assertThat(out.roiPct()).isNull();
        assertThat(out.bepUnits()).isNull();
    }

    @Test
    @DisplayName("loss 케이스 — owners 1000, $9.99, 비용 $100K → ROI 음수")
    void lossCase() {
        var out = calc.calculate(new Scenario(1000, 999), 10_000_000);
        // gross = 1000 × 9.99 = 9990
        // net = 9990 × 0.95 × 0.70 = 6,643.35
        // profit = 6,643.35 - 100,000 = -93,356.65
        // ROI = -93,356.65 / 100,000 × 100 = -93.36
        assertThat(out.profitUsd()).isLessThan(java.math.BigDecimal.ZERO);
        assertThat(out.roiPct()).isLessThan(0.0);
    }

    @Test
    @DisplayName("BEP — 가격 비싸면 BEP 단위 작아짐 (적게 팔아도 본전)")
    void bep_priceImpact() {
        var lowPrice = calc.calculate(new Scenario(10000, 999), 10_000_000);   // $9.99, $100K
        var highPrice = calc.calculate(new Scenario(10000, 5999), 10_000_000); // $59.99, $100K
        assertThat(highPrice.bepUnits()).isLessThan(lowPrice.bepUnits());
    }

    @Test
    @DisplayName("iterateNetUsd (Monte Carlo hot loop) — BigDecimal 결과와 일치 within rounding")
    void iterateNet_matchesBigDecimal() {
        double net = calc.iterateNetUsd(100_000, 19.99);
        // expected: 1,329,335.0 ± float error
        assertThat(net).isCloseTo(1_329_335.0, org.assertj.core.api.Assertions.within(0.5));
    }
}
