package com.gametrend.insight.infrastructure.persistence.verification;

import com.gametrend.insight.domain.verification.CaseStatus;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "verification_case")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String concept;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String hypothesis;

    @Column(name = "target_persona", columnDefinition = "TEXT")
    private String targetPersona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Column(name = "is_priority", nullable = false)
    private boolean priority;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
