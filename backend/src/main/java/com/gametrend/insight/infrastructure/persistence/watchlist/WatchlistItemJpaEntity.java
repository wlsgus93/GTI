package com.gametrend.insight.infrastructure.persistence.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "watchlist_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_watchlist_user_game",
                columnNames = {"user_id", "game_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchlistItemJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "game_id", nullable = false)
    private Long gameId;

    @Column(length = 500)
    private String note;

    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private Instant addedAt;

    public static WatchlistItemJpaEntity newInstance(long userId, long gameId, String note) {
        WatchlistItemJpaEntity e = new WatchlistItemJpaEntity();
        e.userId = userId;
        e.gameId = gameId;
        e.note = note;
        return e;
    }
}
