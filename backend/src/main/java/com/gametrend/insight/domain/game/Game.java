package com.gametrend.insight.domain.game;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Game 도메인 모델 (Pure POJO, Spring/JPA 의존성 X).
 *
 * <p>Steam appid 우선 식별자, IGDB id는 보조. 둘 다 nullable이지만 적어도 하나는 있어야 한다.
 *
 * @param id              내부 PK (DB 저장 후 부여)
 * @param steamAppId      Steam 고유 ID (nullable)
 * @param igdbId          IGDB 고유 ID (nullable)
 * @param name            게임 명 (필수)
 * @param description     설명 (nullable)
 * @param releaseDate     출시일 (nullable)
 * @param developer       개발사 (nullable)
 * @param publisher       퍼블리셔 (nullable)
 * @param coverImageUrl   커버 이미지 URL (nullable)
 * @param genres          장르 집합 (불변)
 * @param createdAt       생성 시각 (UTC)
 * @param updatedAt       마지막 수정 시각 (UTC)
 */
public record Game(
        Long id,
        Long steamAppId,
        Long igdbId,
        String name,
        String description,
        LocalDate releaseDate,
        String developer,
        String publisher,
        String coverImageUrl,
        Set<Genre> genres,
        Instant createdAt,
        Instant updatedAt) {

    public Game {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Game name must not be blank");
        }
        if (steamAppId == null && igdbId == null) {
            throw new IllegalArgumentException("At least one of steamAppId or igdbId must be present");
        }
        genres = genres == null ? Set.of() : Set.copyOf(genres);
    }
}
