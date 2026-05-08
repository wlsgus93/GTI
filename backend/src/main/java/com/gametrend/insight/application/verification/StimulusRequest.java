package com.gametrend.insight.application.verification;

import com.gametrend.insight.domain.verification.StimulusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /verification/cases/{code}/stimuli — 자극물 등록 요청. */
public record StimulusRequest(
        @NotNull StimulusType type,
        @NotBlank @Size(max = 255) String title,
        @Size(max = 500) String url,
        String description) {
}
