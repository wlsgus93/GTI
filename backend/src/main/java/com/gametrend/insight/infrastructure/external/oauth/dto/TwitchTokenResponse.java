package com.gametrend.insight.infrastructure.external.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Twitch OAuth Client Credentials grant 응답.
 *
 * <p>예시:
 * <pre>{@code
 * {"access_token": "abc...", "expires_in": 5184000, "token_type": "bearer"}
 * }</pre>
 */
public record TwitchTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("token_type") String tokenType) {
}
