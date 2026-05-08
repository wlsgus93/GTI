package com.gametrend.insight.infrastructure.persistence.verification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campaign_metric")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignMetricJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long clicks;

    @Column(nullable = false)
    private long conversions;

    @Column(name = "spent_cents", nullable = false)
    private long spentCents;
}
