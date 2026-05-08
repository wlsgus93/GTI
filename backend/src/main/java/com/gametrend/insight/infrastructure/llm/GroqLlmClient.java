package com.gametrend.insight.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import java.util.List;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Groq LPU LLM 클라이언트 (OpenAI 호환 chat completions).
 *
 * <p>API: {@code POST /openai/v1/chat/completions}
 *
 * <p>FAST task 전용 — sentiment 분류, 키워드 추출, 번역, 정규화. 수백 tps + 14400 RPD 무료.
 */
public final class GroqLlmClient implements LlmClient {

    private final WebClient webClient;
    private final GroqProperties props;

    public GroqLlmClient(WebClient webClient, GroqProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens) {
        ChatRequest request = new ChatRequest(
                props.model(),
                List.of(
                        new ChatMessage("system", systemPrompt),
                        new ChatMessage("user", userPrompt)),
                maxTokens,
                props.temperature());

        try {
            ChatResponse resp = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + props.apiKey())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatResponse.class)
                    .block();

            if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
                throw new IllegalStateException("Groq empty response");
            }
            Choice c = resp.choices().get(0);
            String content = c.message() == null ? "" : c.message().content();

            Usage usage = resp.usage();
            int promptTokens = usage == null || usage.promptTokens() == null ? 0 : usage.promptTokens();
            int completionTokens = usage == null || usage.completionTokens() == null ? 0 : usage.completionTokens();

            return new LlmResponse(content, promptTokens, completionTokens, props.model());
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Groq call failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    // ==== OpenAI-compatible chat completions schema ====

    public record ChatRequest(
            String model,
            List<ChatMessage> messages,
            @com.fasterxml.jackson.annotation.JsonProperty("max_tokens") Integer maxTokens,
            Double temperature) {}

    public record ChatMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(
            String id, String model, List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            Integer index,
            ChatMessage message,
            @com.fasterxml.jackson.annotation.JsonProperty("finish_reason") String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Usage(
            @com.fasterxml.jackson.annotation.JsonProperty("prompt_tokens") Integer promptTokens,
            @com.fasterxml.jackson.annotation.JsonProperty("completion_tokens") Integer completionTokens,
            @com.fasterxml.jackson.annotation.JsonProperty("total_tokens") Integer totalTokens) {}
}
