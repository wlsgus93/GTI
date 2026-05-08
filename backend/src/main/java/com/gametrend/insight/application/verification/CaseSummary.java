package com.gametrend.insight.application.verification;

import com.gametrend.insight.domain.verification.CaseStatus;

/** GET /verification/cases — 목록 응답 항목. */
public record CaseSummary(
        Long id,
        String code,
        String title,
        CaseStatus status,
        boolean priority,
        int stimulusCount,
        int campaignCount) {
}
