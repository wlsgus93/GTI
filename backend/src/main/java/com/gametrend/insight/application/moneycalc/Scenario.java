package com.gametrend.insight.application.moneycalc;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 단일 시나리오 입력 — 3-pt 추정의 한 점 (비관/보통/낙관 중 하나).
 *
 * @param owners      예상 판매 단위 (또는 SteamSpy owners)
 * @param priceCents  가격 (USD cents)
 */
public record Scenario(
        @PositiveOrZero long owners,
        @Positive long priceCents) {
}
