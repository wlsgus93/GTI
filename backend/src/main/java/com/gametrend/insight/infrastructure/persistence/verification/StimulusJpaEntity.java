package com.gametrend.insight.infrastructure.persistence.verification;

import com.gametrend.insight.domain.verification.StimulusType;
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
@Table(name = "stimulus")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StimulusJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StimulusType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** application 레이어가 새 엔티티 생성 시 사용 (Lombok protected ctor 우회). */
    public static StimulusJpaEntity newInstance(
            Long caseId, StimulusType type, String title, String url, String description) {
        StimulusJpaEntity e = new StimulusJpaEntity();
        e.caseId = caseId;
        e.type = type;
        e.title = title;
        e.url = url;
        e.description = description;
        return e;
    }
}
