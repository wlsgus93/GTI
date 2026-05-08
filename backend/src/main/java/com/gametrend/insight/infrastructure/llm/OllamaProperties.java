package com.gametrend.insight.infrastructure.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로컬 Ollama LLM 설정.
 *
 * <p>Spring AI Ollama starter 대신 직접 WebClient + REST 호출 — starter 가 ChatClient.Builder
 * 자동 매핑을 점유해 Anthropic starter 와 충돌 가능. 수동 통합으로 둘 다 공존.
 *
 * <p>활성화 조건: {@code gti.llm.provider=ollama}.
 *
 * @param baseUrl     Ollama base URL (예: {@code http://localhost:11434})
 * @param model       모델 ID (예: {@code qwen2.5:7b-instruct-q4_K_M})
 * @param timeoutMs   HTTP timeout (cold model 첫 호출은 길 수 있음 — 60s+ 권장)
 * @param temperature 응답 다양성 (0.0~1.0, 페르소나 분기는 0.6~0.8 권장)
 */
@ConfigurationProperties("gti.llm.ollama")
public record OllamaProperties(
        String baseUrl, String model, int timeoutMs, double temperature) {

    public OllamaProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:11434";
        }
        if (model == null || model.isBlank()) {
            model = "qwen2.5:7b-instruct-q4_K_M";
        }
        if (timeoutMs <= 0) {
            timeoutMs = 60_000;
        }
        if (temperature <= 0) {
            temperature = 0.7;
        }
    }
}
