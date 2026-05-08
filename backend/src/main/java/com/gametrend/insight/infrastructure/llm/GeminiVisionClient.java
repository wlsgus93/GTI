package com.gametrend.insight.infrastructure.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Gemini Vision 클라이언트 — D3 그래픽 성향 전용.
 *
 * <p>흐름:
 * <ol>
 *   <li>이미지 URL 다운로드 (WebClient)
 *   <li>base64 인코딩 + MIME 추정
 *   <li>Gemini generateContent 호출 (text + inline_data)
 * </ol>
 *
 * <p>{@link GeminiLlmClient} (text only) 와 별개 — Vision 전용 path. {@link com.gametrend.insight.application.insight.LlmClient}
 * 인터페이스 확장 X (Vision 은 호출 schema 가 다른 multipart).
 */
public final class GeminiVisionClient {

    private static final int MAX_IMAGE_BYTES = 4 * 1024 * 1024; // 4MB
    private static final String DEFAULT_MIME = "image/jpeg";

    private final WebClient geminiWebClient;
    private final WebClient imageDownloadClient;
    private final GeminiProperties props;

    public GeminiVisionClient(WebClient geminiWebClient, WebClient.Builder builder, GeminiProperties props) {
        this.geminiWebClient = geminiWebClient;
        this.imageDownloadClient = builder.clone()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IMAGE_BYTES))
                .build();
        this.props = props;
    }

    /**
     * 이미지 URL → Vision 분석 결과 텍스트.
     *
     * @param imageUrl    분석 대상 이미지 URL
     * @param systemPrompt 시스템 지시 (분류 schema 강제 등)
     * @param userPrompt  사용자 질문
     * @param maxTokens   max output tokens
     * @return Gemini 응답 텍스트 (모델 + 토큰)
     */
    public VisionResponse analyze(String imageUrl, String systemPrompt, String userPrompt, int maxTokens) {
        byte[] image = downloadImage(imageUrl);
        String base64 = Base64.getEncoder().encodeToString(image);
        String mime = guessMime(imageUrl);

        VisionRequest request = new VisionRequest(
                systemPrompt == null
                        ? null
                        : new GeminiLlmClient.SystemInstruction(List.of(new GeminiLlmClient.Part(systemPrompt))),
                List.of(new VisionContent("user", List.of(
                        new VisionPart(userPrompt, null),
                        new VisionPart(null, new InlineData(mime, base64))))),
                new GeminiLlmClient.GenerationConfig(maxTokens, props.temperature()));

        String uri = "/models/" + props.model() + ":generateContent?key=" + props.apiKey();

        try {
            GeminiLlmClient.GeminiResponse resp = geminiWebClient.post()
                    .uri(uri)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiLlmClient.GeminiResponse.class)
                    .block();
            if (resp == null || resp.candidates() == null || resp.candidates().isEmpty()) {
                throw new IllegalStateException("Gemini Vision empty response");
            }
            GeminiLlmClient.Candidate c = resp.candidates().get(0);
            String content = (c.content() == null || c.content().parts() == null
                    || c.content().parts().isEmpty())
                    ? ""
                    : c.content().parts().get(0).text();
            int prompt = resp.usageMetadata() == null || resp.usageMetadata().promptTokenCount() == null
                    ? 0 : resp.usageMetadata().promptTokenCount();
            int completion = resp.usageMetadata() == null || resp.usageMetadata().candidatesTokenCount() == null
                    ? 0 : resp.usageMetadata().candidatesTokenCount();
            return new VisionResponse(content, prompt, completion, props.model());
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Gemini Vision call failed: " + e.getStatusCode() + " " + e.getResponseBodyAsString(), e);
        }
    }

    private byte[] downloadImage(String url) {
        try {
            byte[] bytes = imageDownloadClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("Empty image download: " + url);
            }
            return bytes;
        } catch (WebClientResponseException e) {
            throw new IllegalStateException(
                    "Image download failed: " + e.getStatusCode() + " url=" + url, e);
        }
    }

    private static String guessMime(String url) {
        String lower = url.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return DEFAULT_MIME;
    }

    public record VisionResponse(
            String content,
            int promptTokens,
            int completionTokens,
            String model) {}

    // === Vision-specific schema (text + inline_data multipart) ===

    @JsonInclude(Include.NON_NULL)
    public record VisionRequest(
            GeminiLlmClient.SystemInstruction systemInstruction,
            List<VisionContent> contents,
            GeminiLlmClient.GenerationConfig generationConfig) {}

    public record VisionContent(String role, List<VisionPart> parts) {}

    @JsonInclude(Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VisionPart(
            String text,
            @JsonProperty("inline_data") InlineData inlineData) {}

    public record InlineData(
            @JsonProperty("mime_type") String mimeType,
            String data) {}
}
