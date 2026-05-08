package com.gametrend.insight.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Google AI Studio (Gemini) LLM 클라이언트.
 *
 * <p>API: {@code POST /v1beta/models/{model}:generateContent?key={apiKey}}
 *
 * <p>HIGH_QUALITY task 전용 — 페르소나 분기, Strategy 추천, Vision (D3), Audio (P7).
 * 한국어 1티어 + 1M 컨텍스트 + Vision/Audio 멀티모달.
 */
public final class GeminiLlmClient implements LlmClient {

    private final WebClient webClient;
    private final GeminiProperties props;

    public GeminiLlmClient(WebClient webClient, GeminiProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens) {
        // Gemini 는 system role 미지원 — system instruction 으로 별도 필드.
        GeminiRequest request = new GeminiRequest(
                systemPrompt == null
                        ? null
                        : new SystemInstruction(List.of(new Part(systemPrompt))),
                List.of(new Content("user", List.of(new Part(userPrompt)))),
                new GenerationConfig(maxTokens, props.temperature()));

        String uri = "/models/" + props.model() + ":generateContent?key=" + props.apiKey();

        try {
            GeminiResponse resp = webClient.post()
                    .uri(uri)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block();

            if (resp == null || resp.candidates() == null || resp.candidates().isEmpty()) {
                throw new IllegalStateException("Gemini empty response");
            }
            Candidate c = resp.candidates().get(0);
            String content = (c.content() == null || c.content().parts() == null
                    || c.content().parts().isEmpty())
                    ? ""
                    : c.content().parts().get(0).text();

            UsageMetadata usage = resp.usageMetadata();
            int promptTokens = usage == null || usage.promptTokenCount() == null ? 0 : usage.promptTokenCount();
            int completionTokens = usage == null || usage.candidatesTokenCount() == null ? 0 : usage.candidatesTokenCount();

            return new LlmResponse(content, promptTokens, completionTokens, props.model());
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Gemini call failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    // ==== Gemini REST schema (text-only) ====

    public record GeminiRequest(
            SystemInstruction systemInstruction,
            List<Content> contents,
            GenerationConfig generationConfig) {}

    public record SystemInstruction(List<Part> parts) {}

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(Integer maxOutputTokens, Double temperature) {
        @com.fasterxml.jackson.annotation.JsonProperty("maxOutputTokens")
        public Integer maxOutputTokens() {
            return maxOutputTokens;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiResponse(List<Candidate> candidates, UsageMetadata usageMetadata) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, String finishReason, Integer index) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsageMetadata(
            Integer promptTokenCount,
            Integer candidatesTokenCount,
            Integer totalTokenCount,
            List<Map<String, Object>> promptTokensDetails) {}
}
