package com.gametrend.insight.infrastructure.external.googleplay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * crawler-service `/charts/google-play` 응답.
 *
 * <p>예시:
 * <pre>{@code
 * {
 *   "country": "us",
 *   "category": "GAME",
 *   "collection": "TOP_FREE",
 *   "fetchedAt": "2026-...",
 *   "items": [
 *     {"rank":1,"appId":"com.example.game","title":"...","developer":"...","score":4.5,"free":true}
 *   ]
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GooglePlayChartsResponse(
        String country,
        String category,
        String collection,
        String fetchedAt,
        List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            int rank,
            String appId,
            String title,
            String developer,
            Double score,
            Boolean free) {}
}
