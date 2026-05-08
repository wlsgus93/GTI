package com.gametrend.insight.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * 로컬 Ollama LLM 클라이언트 — REST 직접 호출.
 *
 * <p>Spring AI Ollama starter 대신 수동 통합 — Anthropic starter 와 ChatClient.Builder 점유 충돌 회피.
 *
 * <p>POST {@code /api/chat} ({@code stream: false}) — 단일 응답으로 받음. SSE streaming 은 W7+ 후속.
 *
 * <p>Activation: {@link com.gametrend.insight.config.LlmConfig} 가 {@code gti.llm.provider=ollama}
 * 시 빈 등록.
 *
 * <p>한계 (Claude 대비):
 * <ul>
 *   <li>4 페르소나 분기 톤 차이 약함 (7B 모델 한계)
 *   <li>한국어 게임 도메인 정확도 ↓
 *   <li>할루시네이션 검증 통과율 ↓ — 룰 {@code 94-data-analyst-safety-global.mdc} 재호출 필요할 수 있음
 *   <li>JSON 형식 강제 약함 (function calling 안 씀)
 * </ul>
 */
public final class OllamaLlmClient implements LlmClient {

    private final WebClient webClient;
    private final OllamaProperties props;

    public OllamaLlmClient(WebClient webClient, OllamaProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens) {
        OllamaChatRequest request = new OllamaChatRequest(
                props.model(),
                List.of(
                        new OllamaMessage("system", systemPrompt),
                        new OllamaMessage("user", userPrompt)),
                false,
                Map.of(
                        "num_predict", maxTokens,
                        "temperature", props.temperature()));

        try {
            OllamaChatResponse resp = webClient.post()
                    .uri("/api/chat")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .block();

            if (resp == null || resp.message() == null) {
                throw new IllegalStateException("Ollama empty response");
            }
            return new LlmResponse(
                    resp.message().content(),
                    resp.promptEvalCount() == null ? 0 : resp.promptEvalCount(),
                    resp.evalCount() == null ? 0 : resp.evalCount(),
                    resp.model() == null ? props.model() : resp.model());
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Ollama call failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    // ==== Ollama REST schema ====

    public record OllamaChatRequest(
            String model, List<OllamaMessage> messages, boolean stream, Map<String, Object> options) {}

    public record OllamaMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaChatResponse(
            String model,
            String createdAt,
            OllamaMessage message,
            Boolean done,
            Long totalDuration,
            Long loadDuration,
            Integer promptEvalCount,
            Long promptEvalDuration,
            Integer evalCount,
            Long evalDuration) {}
}
