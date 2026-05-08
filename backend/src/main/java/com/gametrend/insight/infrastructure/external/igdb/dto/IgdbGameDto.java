package com.gametrend.insight.infrastructure.external.igdb.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * IGDB {@code /games} 단일 응답 항목.
 *
 * <p>Apicalypse 쿼리 예: {@code fields name,summary,first_release_date,genres.name,cover.url; where id = 1942;}
 *
 * <p>응답은 항상 배열 ({@code IgdbGameDto[]}) — id로 조회해도 길이 0 또는 1.
 */
public record IgdbGameDto(
        long id,
        String name,
        String summary,
        @JsonProperty("first_release_date") Long firstReleaseDate, // Unix epoch seconds
        List<Genre> genres,
        Cover cover) {

    public record Genre(long id, String name) {}

    public record Cover(long id, String url) {}
}
