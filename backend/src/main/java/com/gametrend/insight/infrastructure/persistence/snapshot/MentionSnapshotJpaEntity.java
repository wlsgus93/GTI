package com.gametrend.insight.infrastructure.persistence.snapshot;

import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
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
@Table(name = "mention_snapshot")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentionSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "mention_count", nullable = false)
    private int mentionCount;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private MentionSnapshot.Sentiment sentiment;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SnapshotSource source;

    @Column(nullable = false)
    private boolean stale;

    public MentionSnapshot toDomain() {
        return new MentionSnapshot(id, gameId, mentionCount, sentiment, capturedAt, source, stale);
    }

    public static MentionSnapshotJpaEntity from(MentionSnapshot s) {
        MentionSnapshotJpaEntity e = new MentionSnapshotJpaEntity();
        e.id = s.id();
        e.gameId = s.gameId();
        e.mentionCount = s.mentionCount();
        e.sentiment = s.sentiment();
        e.capturedAt = s.capturedAt();
        e.source = s.source();
        e.stale = s.stale();
        return e;
    }
}
