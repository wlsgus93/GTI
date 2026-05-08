package com.gametrend.insight.application.moneycalc;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 게임 발매 매출 시뮬레이션 요청 — 3-pt 추정 + 비용 + Monte Carlo 옵션.
 *
 * <p>비즈니스 검증 (validate()):
 * <ul>
 *   <li>{@code pessimistic.owners ≤ realistic.owners ≤ optimistic.owners}
 *   <li>{@code monteCarloIterations}: 100~10,000 (DoS 방어)
 * </ul>
 */
public record MoneyCalcRequest(
        @NotNull @Valid Scenario pessimistic,
        @NotNull @Valid Scenario realistic,
        @NotNull @Valid Scenario optimistic,
        @PositiveOrZero long developmentCostCents,
        @PositiveOrZero long marketingCostCents,
        @Min(100) Integer monteCarloIterations) {

    /** Bean Validation 외 비즈니스 검증. {@link #validateBusiness()} 호출. */
    public void validateBusiness() {
        if (pessimistic.owners() > realistic.owners()) {
            throw new IllegalArgumentException(
                    "pessimistic.owners must be ≤ realistic.owners (got "
                            + pessimistic.owners() + " > " + realistic.owners() + ")");
        }
        if (realistic.owners() > optimistic.owners()) {
            throw new IllegalArgumentException(
                    "realistic.owners must be ≤ optimistic.owners (got "
                            + realistic.owners() + " > " + optimistic.owners() + ")");
        }
        if (monteCarloIterations != null && monteCarloIterations > 10_000) {
            throw new IllegalArgumentException(
                    "monteCarloIterations must be ≤ 10000 (got " + monteCarloIterations + ")");
        }
    }

    public int iterationsOrDefault() {
        return monteCarloIterations == null ? 1000 : monteCarloIterations;
    }

    public long totalCostCents() {
        return developmentCostCents + marketingCostCents;
    }
}
