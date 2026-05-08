package com.gametrend.insight.infrastructure.external.steam;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Steam 외부 API 설정. {@code application.yml}의 {@code gti.external.steam.*}와 매핑.
 *
 * @param baseUrl       Steam Web API base (https://api.steampowered.com)
 * @param storefrontUrl Steam Storefront base (Day 4에서 사용)
 * @param apiKey        Steam Web API Key (선택적 — 일부 엔드포인트는 키 없이도 동작)
 * @param timeoutMs     소스별 read timeout (ms)
 */
@ConfigurationProperties(prefix = "gti.external.steam")
public record SteamProperties(
        String baseUrl,
        String storefrontUrl,
        String apiKey,
        Integer timeoutMs) {
}
