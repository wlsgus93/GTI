package com.gametrend.insight.infrastructure.persistence.snapshot;

import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
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
@Table(name = "player_snapshot")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlayerSnapshotJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(name = "concurrent_players")
    private Integer concurrentPlayers;

    @Column(name = "review_score_positive")
    private Integer reviewScorePositive;

    @Column(name = "review_score_total")
    private Integer reviewScoreTotal;

    /** SteamSpy 소유자 범위 하한. */
    @Column(name = "owners_low")
    private Long ownersLow;

    /** SteamSpy 소유자 범위 상한. */
    @Column(name = "owners_high")
    private Long ownersHigh;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SnapshotSource source;

    @Column(nullable = false)
    private boolean stale;

    public PlayerSnapshot toDomain() {
        return new PlayerSnapshot(
                id,
                gameId,
                concurrentPlayers,
                reviewScorePositive,
                reviewScoreTotal,
                ownersLow,
                ownersHigh,
                capturedAt,
                source,
                stale);
    }

    public static PlayerSnapshotJpaEntity from(PlayerSnapshot s) {
        PlayerSnapshotJpaEntity e = new PlayerSnapshotJpaEntity();
        e.id = s.id();
        e.gameId = s.gameId();
        e.concurrentPlayers = s.concurrentPlayers();
        e.reviewScorePositive = s.reviewScorePositive();
        e.reviewScoreTotal = s.reviewScoreTotal();
        e.ownersLow = s.ownersLow();
        e.ownersHigh = s.ownersHigh();
        e.capturedAt = s.capturedAt();
        e.source = s.source();
        e.stale = s.stale();
        return e;
    }
}
