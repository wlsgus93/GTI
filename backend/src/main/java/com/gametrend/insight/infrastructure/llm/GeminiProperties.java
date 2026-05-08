package com.gametrend.insight.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google AI Studio (Gemini) 설정.
 *
 * <p>API: {@code https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={apiKey}}
 *
 * <p>무료 한도: 1000 RPD (Flash) / 100 RPD (Pro). 한국어 + Vision + 1M context.
 *
 * @param apiKey      Google AI Studio API key (https://aistudio.google.com/app/apikey)
 * @param baseUrl     base URL — 보통 default 그대로
 * @param model       모델 ID (예: {@code gemini-2.5-flash})
 * @param timeoutMs   HTTP timeout
 * @param temperature 응답 다양성 (0.0~2.0)
 */
@ConfigurationProperties("gti.llm.gemini")
public record GeminiProperties(
        String apiKey, String baseUrl, String model, int timeoutMs, double temperature) {

    public GeminiProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        }
        if (model == null || model.isBlank()) {
            model = "gemini-2.5-flash";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 30_000;
        }
        if (temperature <= 0) {
            temperature = 0.7;
        }
    }
}
