package com.gametrend.insight.infrastructure.external.apple;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Apple iTunes legacy RSS 응답 (Atom-style with {@code im:*} prefixed properties).
 *
 * <p>URL: {@code https://itunes.apple.com/{country}/rss/topfreeapplications/limit={N}/genre=6014/json}
 *
 * <p>v2 spec (rss.marketingtools.apple.com) 은 응답에 {@code genres:[]} 빈 배열 + query param
 * 게임 카테고리 필터링 미지원 → legacy spec 으로 전환. 게임만 직접 필터링 (genre=6014, server-side).
 *
 * <p>예시 응답 (간략화):
 * <pre>{@code
 * {
 *   "feed": {
 *     "entry": [
 *       {
 *         "im:name": {"label": "Magic Sort!"},
 *         "im:artist": {"label": "Voodoo SAS"},
 *         "id": {"label": "https://...", "attributes": {"im:id": "1234567890"}}
 *       }
 *     ]
 *   }
 * }
 * }</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TopChartsResponse(Feed feed) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Feed(List<Entry> entry) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            @JsonProperty("im:name") Label name,
            @JsonProperty("im:artist") Label artist,
            EntryId id) {

        public String getName() {
            return name != null ? name.label() : null;
        }

        public String getArtistName() {
            return artist != null ? artist.label() : null;
        }

        /** {@code id.attributes.im:id} = Apple App Store appId (예: "1234567890"). */
        public String getAppId() {
            return id != null ? id.getImId() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Label(String label) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EntryId(String label, IdAttrs attributes) {

        public String getImId() {
            return attributes != null ? attributes.imId() : null;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdAttrs(@JsonProperty("im:id") String imId) {}
}
