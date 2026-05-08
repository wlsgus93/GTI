package com.gametrend.insight.infrastructure.persistence.verification;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StimulusJpaRepository extends JpaRepository<StimulusJpaEntity, Long> {

    List<StimulusJpaEntity> findByCaseIdOrderByCreatedAtAsc(Long caseId);
}
