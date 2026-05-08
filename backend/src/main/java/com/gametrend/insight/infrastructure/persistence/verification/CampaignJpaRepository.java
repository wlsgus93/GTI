package com.gametrend.insight.infrastructure.persistence.verification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignJpaRepository extends JpaRepository<CampaignJpaEntity, Long> {

    List<CampaignJpaEntity> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
