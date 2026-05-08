package com.gametrend.insight.infrastructure.external.googleplay;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Play crawler-service 설정.
 *
 * <p>비공식 google-play-scraper 호출은 별도 Node.js 마이크로서비스(crawler-service) 가 담당.
 * Spring 본체는 그 서비스의 REST endpoint 만 호출.
 *
 * @param baseUrl   crawler-service base URL (예: "http://localhost:3001" 또는 docker "http://crawler-service:3001")
 * @param timeoutMs HTTP timeout (ms)
 */
@ConfigurationProperties("gti.external.google-play")
public record GooglePlayChartsProperties(String baseUrl, int timeoutMs) {

    public GooglePlayChartsProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:3001";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 10_000;
        }
    }
}
