package com.gametrend.insight.application.verification;

/** JPQL constructor projection — 캠페인별 누적 메트릭 집계. */
public record CampaignAggregate(
        Long campaignId,
        long totalImpressions,
        long totalClicks,
        long totalConversions,
        long totalSpentCents) {

    public Double ctr() {
        if (totalImpressions == 0) return null;
        return round4((double) totalClicks / totalImpressions);
    }

    public Double cvr() {
        if (totalClicks == 0) return null;
        return round4((double) totalConversions / totalClicks);
    }

    /** CPM = 1000 impressions당 비용 (USD cents). */
    public Long cpmCents() {
        if (totalImpressions == 0) return null;
        return totalSpentCents * 1000 / totalImpressions;
    }

    /** CPC = 클릭당 비용 (USD cents). */
    public Long cpcCents() {
        if (totalClicks == 0) return null;
        return totalSpentCents / totalClicks;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
