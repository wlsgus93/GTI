package com.gametrend.insight.application.verification;

import com.gametrend.insight.domain.verification.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** POST /verification/cases/{code}/campaigns — 캠페인 시작 요청. */
public record CampaignRequest(
        Long stimulusId,
        @NotNull Platform platform,
        @NotBlank @Size(max = 255) String name,
        @Size(max = 100) String utmCampaign,
        @PositiveOrZero long budgetCents) {
}
