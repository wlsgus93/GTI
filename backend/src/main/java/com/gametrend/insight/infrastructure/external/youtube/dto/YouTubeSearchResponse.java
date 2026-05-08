package com.gametrend.insight.infrastructure.external.youtube.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * YouTube Data v3 {@code GET /search} 응답 (필요한 필드만).
 *
 * <p>예시:
 * <pre>{@code
 * {"pageInfo": {"totalResults": 12345, "resultsPerPage": 50}, "items": [...]}
 * }</pre>
 *
 * <p>{@code totalResults}만 사용 — 정확한 카운트는 아니지만 인기 변화 추적용 근사치.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record YouTubeSearchResponse(@JsonProperty("pageInfo") PageInfo pageInfo) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(@JsonProperty("totalResults") int totalResults) {}
}
