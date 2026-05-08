package com.gametrend.insight.infrastructure.persistence.verification;

import com.gametrend.insight.domain.verification.CampaignStatus;
import com.gametrend.insight.domain.verification.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "campaign")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    /** 자극물 연결 (nullable). */
    @Column(name = "stimulus_id")
    private Long stimulusId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "utm_campaign", length = 100)
    private String utmCampaign;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "budget_cents", nullable = false)
    private long budgetCents;

    @Column(name = "spent_cents", nullable = false)
    private long spentCents;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static CampaignJpaEntity newInstance(
            Long caseId, Long stimulusId, Platform platform, String name,
            String utmCampaign, CampaignStatus status, long budgetCents) {
        CampaignJpaEntity e = new CampaignJpaEntity();
        e.caseId = caseId;
        e.stimulusId = stimulusId;
        e.platform = platform;
        e.name = name;
        e.utmCampaign = utmCampaign;
        e.status = status;
        e.budgetCents = budgetCents;
        e.spentCents = 0;
        return e;
    }
}
