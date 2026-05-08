package com.gametrend.insight.infrastructure.persistence.verification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationCaseJpaRepository extends JpaRepository<VerificationCaseJpaEntity, Long> {

    Optional<VerificationCaseJpaEntity> findByCode(String code);
}
