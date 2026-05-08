package com.gametrend.insight.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Groq LPU API 설정 (OpenAI 호환).
 *
 * <p>API: {@code https://api.groq.com/openai/v1/chat/completions}
 *
 * <p>무료 한도: 14400 RPD (작은 모델), 수백 tps. sentiment / 키워드 / 번역 등 빠른 task.
 *
 * @param apiKey      Groq API key (https://console.groq.com/keys)
 * @param baseUrl     base URL — 보통 default 그대로
 * @param model       모델 ID (예: {@code llama-3.3-70b-versatile})
 * @param timeoutMs   HTTP timeout
 * @param temperature 응답 다양성 (0.0~2.0)
 */
@ConfigurationProperties("gti.llm.groq")
public record GroqProperties(
        String apiKey, String baseUrl, String model, int timeoutMs, double temperature) {

    public GroqProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.groq.com/openai/v1";
        }
        if (model == null || model.isBlank()) {
            model = "llama-3.3-70b-versatile";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 15_000;
        }
        if (temperature <= 0) {
            temperature = 0.5;
        }
    }
}
