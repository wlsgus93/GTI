package com.gametrend.insight.application.insight;

/**
 * LLM 응답. 본문 + 토큰 사용량 + 모델 식별자.
 *
 * <p>토큰 추적 목적:
 * <ul>
 *   <li>비용 추적 (Anthropic 단가 × tokens)
 *   <li>프롬프트 최적화 효과 측정 (버전별 토큰량 비교)
 * </ul>
 */
public record LlmResponse(
        String content,
        int promptTokens,
        int completionTokens,
        String model) {

    public int totalTokens() {
        return promptTokens + completionTokens;
    }
}
