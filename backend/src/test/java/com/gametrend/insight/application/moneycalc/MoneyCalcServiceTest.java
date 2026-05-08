package com.gametrend.insight.application.moneycalc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyCalcServiceTest {

    private final MoneyCalcService service = new MoneyCalcService(
            new MoneyCalcCalculator(),
            new MonteCarloSimulator(new MoneyCalcCalculator()),
            new SensitivityAnalyzer(new MoneyCalcCalculator()));

    @Test
    @DisplayName("정상 시뮬레이션 — 3 시나리오 + Monte Carlo + 민감도 모두 채워짐")
    void fullSimulation() {
        var req = new MoneyCalcRequest(
                new Scenario(50_000, 1999),
                new Scenario(200_000, 1999),
                new Scenario(800_000, 1999),
                50_000_000L, 10_000_000L, 1000);

        var result = service.simulate(req);

        assertThat(result.pessimistic()).isNotNull();
        assertThat(result.realistic()).isNotNull();
        assertThat(result.optimistic()).isNotNull();
        assertThat(result.monteCarlo()).isNotNull();
        assertThat(result.monteCarlo().iterations()).isEqualTo(1000);
        assertThat(result.sensitivity()).hasSize(2);
        assertThat(result.assumptions().refundRate()).isEqualTo(0.05);
        assertThat(result.assumptions().steamCut()).isEqualTo(0.30);
        assertThat(result.assumptions().randomSeed()).isEqualTo(42L);

        // 단조성: pessimistic.net < realistic.net < optimistic.net
        assertThat(result.pessimistic().developerNetUsd())
                .isLessThan(result.realistic().developerNetUsd());
        assertThat(result.realistic().developerNetUsd())
                .isLessThan(result.optimistic().developerNetUsd());
    }

    @Test
    @DisplayName("결정성 — 같은 입력 + 같은 seed → 같은 결과")
    void deterministicResult() {
        var req = sampleReq();
        var r1 = service.simulate(req, 42L);
        var r2 = service.simulate(req, 42L);

        assertThat(r1.monteCarlo().netRevenueMean()).isEqualByComparingTo(r2.monteCarlo().netRevenueMean());
        assertThat(r1.sensitivity().get(0).impactRatio()).isEqualTo(r2.sensitivity().get(0).impactRatio());
    }

    @Test
    @DisplayName("validateBusiness — pess.owners > real.owners 위반 → IAE")
    void invalidScenarioOrder() {
        var req = new MoneyCalcRequest(
                new Scenario(500_000, 1999), // pess
                new Scenario(100_000, 1999), // real (smaller!)
                new Scenario(800_000, 1999),
                50_000_000L, 0L, null);

        assertThatThrownBy(() -> service.simulate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pessimistic.owners must be ≤ realistic.owners");
    }

    @Test
    @DisplayName("validateBusiness — monteCarloIterations > 10000 → IAE (DoS 방어)")
    void tooManyIterations_throws() {
        var req = new MoneyCalcRequest(
                new Scenario(100, 999),
                new Scenario(200, 999),
                new Scenario(300, 999),
                10_000L, 0L, 50_000);

        assertThatThrownBy(() -> service.simulate(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("≤ 10000");
    }

    private static MoneyCalcRequest sampleReq() {
        return new MoneyCalcRequest(
                new Scenario(50_000, 1999),
                new Scenario(200_000, 1999),
                new Scenario(800_000, 1999),
                50_000_000L, 10_000_000L, 500);
    }
}
