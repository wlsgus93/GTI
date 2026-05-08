package com.gametrend.insight.infrastructure.external.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twitch OAuth 공유 자격증명 — Twitch Helix와 IGDB가 같은 Client ID/Secret을 사용.
 *
 * <p>{@code application.yml}의 {@code gti.external.twitch.*}와 매핑.
 */
@ConfigurationProperties(prefix = "gti.external.twitch")
public record TwitchOAuthProperties(
        String baseUrl,
        String oauthUrl,
        String clientId,
        String clientSecret) {
}
