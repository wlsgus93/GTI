package com.gametrend.insight.application.insight;

/**
 * LLM 호출 추상화 (port). 구현체:
 *
 * <ul>
 *   <li>{@code AnthropicLlmClient} — {@code spring.ai.anthropic.api-key} 설정 시
 *   <li>{@code GeminiLlmClient} — Google AI Studio (한국어 + Vision + 1M context)
 *   <li>{@code GroqLlmClient} — OpenAI 호환, 빠른 task (sentiment, 키워드)
 *   <li>{@code OllamaLlmClient} — 로컬 (RX 6600 + Linux ROCm)
 *   <li>{@code RoutingLlmClient} — {@link LlmTask} hint 따라 위 client 분기
 *   <li>{@code StubLlmClient} — fallback (개발 + 테스트)
 * </ul>
 *
 * <p>호출 패턴:
 * <ul>
 *   <li>{@link #complete(String, String, int)} — task hint 없는 default 호출 (HIGH_QUALITY 가정)
 *   <li>{@link #complete(LlmTask, String, String, int)} — task hint 명시 → RoutingLlmClient 가 분기
 * </ul>
 */
public interface LlmClient {

    LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens);

    /**
     * Task hint 명시 호출. 단일 client (Anthropic/Gemini 등) 는 task 무시 + default {@code complete} 위임.
     * {@code RoutingLlmClient} 만 task 따라 분기.
     */
    default LlmResponse complete(LlmTask task, String systemPrompt, String userPrompt, int maxTokens) {
        return complete(systemPrompt, userPrompt, maxTokens);
    }
}
