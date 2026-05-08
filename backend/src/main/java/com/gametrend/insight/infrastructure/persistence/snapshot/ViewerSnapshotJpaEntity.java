package com.gametrend.insight.infrastructure.persistence.snapshot;

import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.domain.snapshot.ViewerSnapshot;
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
@Table(name = "viewer_snapshot")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViewerSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "concurrent_viewers", nullable = false)
    private int concurrentViewers;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SnapshotSource source;

    @Column(nullable = false)
    private boolean stale;

    public ViewerSnapshot toDomain() {
        return new ViewerSnapshot(id, gameId, concurrentViewers, capturedAt, source, stale);
    }

    public static ViewerSnapshotJpaEntity from(ViewerSnapshot s) {
        ViewerSnapshotJpaEntity e = new ViewerSnapshotJpaEntity();
        e.id = s.id();
        e.gameId = s.gameId();
        e.concurrentViewers = s.concurrentViewers();
        e.capturedAt = s.capturedAt();
        e.source = s.source();
        e.stale = s.stale();
        return e;
    }
}
