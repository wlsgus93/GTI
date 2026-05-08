package com.gametrend.insight.infrastructure.external.twitch.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Twitch Helix {@code GET /helix/streams?game_id=...} 응답.
 *
 * <p>예시:
 * <pre>{@code
 * {
 *   "data": [
 *     {"id": "...", "user_name": "Alice", "viewer_count": 1234, ...},
 *     {"id": "...", "user_name": "Bob",   "viewer_count": 567,  ...}
 *   ],
 *   "pagination": {"cursor": "..."}
 * }
 * }</pre>
 *
 * <p>Day 4는 첫 페이지(최대 100개 스트림)의 시청자 합계만 사용.
 */
public record StreamsResponse(List<Stream> data) {

    public record Stream(
            String id,
            @JsonProperty("user_name") String userName,
            @JsonProperty("viewer_count") int viewerCount) {}
}
