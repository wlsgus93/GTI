package com.gametrend.insight.infrastructure.persistence.verification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampaignMetricJpaRepository extends JpaRepository<CampaignMetricJpaEntity, Long> {

    List<CampaignMetricJpaEntity> findByCampaignIdOrderByCapturedAtAsc(Long campaignId);

    /** 캠페인별 누적 impressions/clicks/conversions/spent — KPI 계산 입력. */
    @Query("""
            SELECT new com.gametrend.insight.application.verification.CampaignAggregate(
                m.campaignId,
                COALESCE(SUM(m.impressions), 0),
                COALESCE(SUM(m.clicks), 0),
                COALESCE(SUM(m.conversions), 0),
                COALESCE(SUM(m.spentCents), 0))
            FROM CampaignMetricJpaEntity m
            WHERE m.campaignId IN :campaignIds
            GROUP BY m.campaignId
            """)
    List<com.gametrend.insight.application.verification.CampaignAggregate> aggregateByCampaignIds(
            @Param("campaignIds") List<Long> campaignIds);
}
