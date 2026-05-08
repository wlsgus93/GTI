package com.gametrend.insight.infrastructure.external.reddit.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Reddit {@code /search.json} 응답 (필요 필드만).
 *
 * <p>예시:
 * <pre>{@code
 * {"kind": "Listing", "data": {"after": "...", "children": [...]}}
 * }</pre>
 *
 * <p>{@code data.children}의 길이를 mention count로 사용 (페이지당 25, 100까지 가능).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RedditSearchResponse(Listing data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Listing(List<Object> children) {}
}
