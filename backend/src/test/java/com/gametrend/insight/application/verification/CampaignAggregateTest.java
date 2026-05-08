package com.gametrend.insight.application.verification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CampaignAggregateTest {

    @Test
    @DisplayName("CTR 계산 — clicks/impressions 4자리 반올림")
    void ctr() {
        var agg = new CampaignAggregate(1L, 100_000, 5400, 0, 0);
        assertThat(agg.ctr()).isEqualTo(0.054); // 5400/100K = 0.054
    }

    @Test
    @DisplayName("CVR 계산 — conversions/clicks")
    void cvr() {
        var agg = new CampaignAggregate(1L, 0, 1000, 80, 0);
        assertThat(agg.cvr()).isEqualTo(0.08);
    }

    @Test
    @DisplayName("CPM — 1000 impressions당 비용 (cents)")
    void cpm() {
        // 100K impressions, $50 spent → CPM = 50 cents/imp × 10 = $0.50/1000 = 50 cents per 1000
        var agg = new CampaignAggregate(1L, 100_000, 0, 0, 50_000);
        // 50,000 cents × 1000 / 100,000 = 500 cents per 1000 impressions
        assertThat(agg.cpmCents()).isEqualTo(500L);
    }

    @Test
    @DisplayName("CPC — 클릭당 비용")
    void cpc() {
        var agg = new CampaignAggregate(1L, 100_000, 1000, 0, 50_000);
        assertThat(agg.cpcCents()).isEqualTo(50L); // 50,000 / 1000
    }

    @Test
    @DisplayName("DivisionByZero 가드 — 분모 0이면 null")
    void divisionByZero_null() {
        var agg = new CampaignAggregate(1L, 0, 0, 0, 0);
        assertThat(agg.ctr()).isNull();
        assertThat(agg.cvr()).isNull();
        assertThat(agg.cpmCents()).isNull();
        assertThat(agg.cpcCents()).isNull();
    }

    @Test
    @DisplayName("clicks 0이지만 impressions 있음 → CTR=0, CVR null (clicks 분모)")
    void clicksZero() {
        var agg = new CampaignAggregate(1L, 100_000, 0, 0, 0);
        assertThat(agg.ctr()).isEqualTo(0.0);
        assertThat(agg.cvr()).isNull(); // clicks=0 → div by zero
    }
}
