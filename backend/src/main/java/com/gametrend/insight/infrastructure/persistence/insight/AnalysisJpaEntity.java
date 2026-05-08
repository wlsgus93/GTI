package com.gametrend.insight.infrastructure.persistence.insight;

import com.gametrend.insight.domain.insight.Analysis;
import com.gametrend.insight.domain.insight.AnalysisKind;
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

@Entity
@Table(name = "analysis")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnalysisJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisKind kind;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(length = 50)
    private String model;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public Analysis toDomain() {
        return new Analysis(id, gameId, kind, promptVersion, content,
                promptTokens, completionTokens, totalTokens, model, createdAt, expiresAt);
    }

    public static AnalysisJpaEntity from(Analysis a) {
        AnalysisJpaEntity e = new AnalysisJpaEntity();
        e.id = a.id();
        e.gameId = a.gameId();
        e.kind = a.kind();
        e.promptVersion = a.promptVersion();
        e.content = a.content();
        e.promptTokens = a.promptTokens();
        e.completionTokens = a.completionTokens();
        e.totalTokens = a.totalTokens();
        e.model = a.model();
        e.createdAt = a.createdAt();
        e.expiresAt = a.expiresAt();
        return e;
    }
}
