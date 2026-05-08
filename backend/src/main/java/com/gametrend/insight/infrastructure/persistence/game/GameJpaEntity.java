package com.gametrend.insight.infrastructure.persistence.game;

import com.gametrend.insight.domain.game.Game;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "games")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "steam_app_id", unique = true)
    private Long steamAppId;

    @Column(name = "igdb_id", unique = true)
    private Long igdbId;

    /** Twitch Helix `/games` 카테고리 ID (string). 운영 시 외부 매핑 필요. */
    @Column(name = "twitch_game_id", length = 50)
    private String twitchGameId;

    /** OpenCritic 게임 ID. 운영 시 외부 매핑 필요. */
    @Column(name = "opencritic_id")
    private Long opencriticId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    private String developer;

    private String publisher;

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "game_genres",
            joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<GenreJpaEntity> genres = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Game toDomain() {
        return new Game(
                id,
                steamAppId,
                igdbId,
                name,
                description,
                releaseDate,
                developer,
                publisher,
                coverImageUrl,
                genres.stream().map(GenreJpaEntity::toDomain).collect(Collectors.toSet()),
                createdAt,
                updatedAt);
    }

    public static GameJpaEntity from(Game g) {
        GameJpaEntity e = new GameJpaEntity();
        e.id = g.id();
        e.steamAppId = g.steamAppId();
        e.igdbId = g.igdbId();
        e.name = g.name();
        e.description = g.description();
        e.releaseDate = g.releaseDate();
        e.developer = g.developer();
        e.publisher = g.publisher();
        e.coverImageUrl = g.coverImageUrl();
        // genres는 별도로 setGenres()로 관리 (이미 영속화된 GenreJpaEntity로)
        return e;
    }
}
