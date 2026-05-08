package com.gametrend.insight.infrastructure.llm;

import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import com.gametrend.insight.application.insight.LlmTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task hint 기반 LLM 라우팅 — 비용/속도/품질 trade-off 자동 최적화.
 *
 * <ul>
 *   <li>HIGH_QUALITY task ({@code PERSONA_INSIGHT}, {@code STRATEGY}, {@code VISION}, {@code AUDIO_TRANSCRIBE})
 *       → Gemini 2.5 Flash (한국어 1티어 + Vision/Audio + 1000 RPD 무료)
 *   <li>FAST task ({@code SENTIMENT}, {@code KEYWORD_EXTRACT}, {@code TRANSLATE}, {@code NORMALIZE})
 *       → Groq Llama 3.3 70B (수백 tps + 14400 RPD 무료)
 * </ul>
 *
 * <p>Default {@link #complete(String, String, int)} (task hint 없는 호출) → HIGH_QUALITY 가정 (Gemini).
 *
 * <p>폴백: {@code highQuality} 호출 실패 시 {@code fast} 로 자동 폴백 (반대도 동일). 양쪽 다 실패하면 throw.
 */
public final class RoutingLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(RoutingLlmClient.class);

    private final LlmClient highQualityClient;
    private final LlmClient fastClient;

    public RoutingLlmClient(LlmClient highQualityClient, LlmClient fastClient) {
        this.highQualityClient = highQualityClient;
        this.fastClient = fastClient;
    }

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens) {
        // task hint 없으면 default = HIGH_QUALITY (인사이트 페이지 등)
        return complete(LlmTask.PERSONA_INSIGHT, systemPrompt, userPrompt, maxTokens);
    }

    @Override
    public LlmResponse complete(LlmTask task, String systemPrompt, String userPrompt, int maxTokens) {
        boolean preferHighQuality = task.isHighQuality();
        LlmClient primary = preferHighQuality ? highQualityClient : fastClient;
        LlmClient fallback = preferHighQuality ? fastClient : highQualityClient;
        String primaryName = preferHighQuality ? "gemini" : "groq";

        try {
            return primary.complete(systemPrompt, userPrompt, maxTokens);
        } catch (RuntimeException e) {
            log.warn("LLM primary {} failed for task {}: {} — falling back",
                    primaryName, task, e.getMessage());
            return fallback.complete(systemPrompt, userPrompt, maxTokens);
        }
    }
}
