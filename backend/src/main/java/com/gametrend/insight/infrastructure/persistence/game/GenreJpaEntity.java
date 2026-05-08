package com.gametrend.insight.infrastructure.persistence.game;

import com.gametrend.insight.domain.game.Genre;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "genres")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GenreJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static GenreJpaEntity of(String name) {
        GenreJpaEntity e = new GenreJpaEntity();
        e.name = name;
        return e;
    }

    public Genre toDomain() {
        return new Genre(id, name, createdAt);
    }
}
