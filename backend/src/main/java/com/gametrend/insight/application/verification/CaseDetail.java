package com.gametrend.insight.application.verification;

import com.gametrend.insight.domain.verification.CampaignStatus;
import com.gametrend.insight.domain.verification.CaseStatus;
import com.gametrend.insight.domain.verification.Platform;
import com.gametrend.insight.domain.verification.StimulusType;
import java.time.Instant;
import java.util.List;

/**
 * GET /verification/cases/{code} — 단일 케이스 + 자극물 + 캠페인(누적 메트릭 포함).
 */
public record CaseDetail(
        Long id,
        String code,
        String title,
        String concept,
        String hypothesis,
        String targetPersona,
        CaseStatus status,
        boolean priority,
        Instant createdAt,
        Instant updatedAt,
        List<StimulusItem> stimuli,
        List<CampaignWithMetrics> campaigns) {

    public record StimulusItem(
            Long id,
            StimulusType type,
            String title,
            String url,
            String description,
            Instant createdAt) {}

    /**
     * 캠페인 + 누적 메트릭 + KPI (CTR/CVR/CPM/CPC).
     *
     * @param ctr   click-through-rate (clicks/impressions, 4자리 반올림)
     * @param cvr   conversion-rate (conversions/clicks)
     * @param cpmCents 1000 impressions당 비용 (cents)
     * @param cpcCents 클릭당 비용 (cents)
     */
    public record CampaignWithMetrics(
            Long id,
            Long stimulusId,
            Platform platform,
            String name,
            String utmCampaign,
            CampaignStatus status,
            Instant startedAt,
            Instant endedAt,
            long budgetCents,
            long spentCents,
            long totalImpressions,
            long totalClicks,
            long totalConversions,
            Double ctr,
            Double cvr,
            Long cpmCents,
            Long cpcCents) {}
}
