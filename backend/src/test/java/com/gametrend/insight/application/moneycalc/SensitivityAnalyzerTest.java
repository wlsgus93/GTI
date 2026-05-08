package com.gametrend.insight.application.moneycalc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SensitivityAnalyzerTest {

    private final SensitivityAnalyzer analyzer = new SensitivityAnalyzer(new MoneyCalcCalculator());

    @Test
    @DisplayName("owners 변동폭이 price 변동폭보다 클 때 — owners impact 더 큼 → 정렬 owners 먼저")
    void ownersDominant() {
        // owners 10× 변동 (10K → 1M), price는 일정 (1999) → owners impact 압도적
        var req = new MoneyCalcRequest(
                new Scenario(10_000, 1999),
                new Scenario(100_000, 1999),
                new Scenario(1_000_000, 1999),
                50_000_000L, 0L, null);

        var impacts = analyzer.analyze(req);

        assertThat(impacts).hasSize(2);
        assertThat(impacts.get(0).variable()).isEqualTo("owners");
        assertThat(impacts.get(0).impactRatio())
                .isGreaterThan(impacts.get(1).impactRatio());
        // priceCents는 변동 없음 → impact 0
        assertThat(impacts.get(1).variable()).isEqualTo("priceCents");
        assertThat(impacts.get(1).impactRatio()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("price 변동만 있을 때 — price impact 더 큼 → 정렬 price 먼저")
    void priceDominant() {
        // owners 일정, price 변동 ($9.99 → $59.99)
        var req = new MoneyCalcRequest(
                new Scenario(100_000, 999),
                new Scenario(100_000, 2999),
                new Scenario(100_000, 5999),
                10_000_000L, 0L, null);

        var impacts = analyzer.analyze(req);

        assertThat(impacts.get(0).variable()).isEqualTo("priceCents");
        assertThat(impacts.get(0).impactRatio()).isGreaterThan(0.0);
        assertThat(impacts.get(1).variable()).isEqualTo("owners");
    }

    @Test
    @DisplayName("realistic net 0 → impactRatio null (DivisionByZero 가드)")
    void zeroRealisticNet() {
        // realistic.owners = 0 → realistic net = 0 → 비율 계산 불가
        var req = new MoneyCalcRequest(
                new Scenario(0, 1999),
                new Scenario(0, 1999),
                new Scenario(100_000, 1999),
                10_000L, 0L, null);

        var impacts = analyzer.analyze(req);
        // realistic.owners=0 → realistic net = 0 → 모든 impact null
        assertThat(impacts).allSatisfy(i -> assertThat(i.impactRatio()).isNull());
    }
}
