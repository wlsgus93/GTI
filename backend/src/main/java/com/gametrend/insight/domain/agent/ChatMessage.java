package com.gametrend.insight.domain.agent;

import com.gametrend.insight.application.agent.IntentClassifier.Intent;
import com.gametrend.insight.application.agent.IntentClassifier.Topic;
import java.time.Instant;
import java.util.List;

/**
 * 채팅 메시지 — Layer 1 분류 메타 + Layer 3 토큰 사용량 통합 추적.
 *
 * <p>비용 추적 (룰 95):
 * <ul>
 *   <li>{@code classifierBlocked=true} — Layer 1 차단 (cloud 호출 X)
 *   <li>{@code cached=true} — L1/L2 캐시 hit (cloud 호출 X)
 *   <li>위 둘 다 false 면 실 cloud 호출 — {@code promptTokens + completionTokens} 누적
 * </ul>
 */
public record ChatMessage(
        Long id,
        Long sessionId,
        ChatRole role,
        String content,
        // Layer 1 분류 결과 (USER 메시지만)
        Topic classifiedTopic,
        Intent classifiedIntent,
        Double classifiedConfidence,
        boolean classifierBlocked,
        // Layer 3 cloud LLM 호출 메타 (ASSISTANT 만)
        List<Long> referencedGameIds,
        Integer promptTokens,
        Integer completionTokens,
        String model,
        boolean cached,
        Integer latencyMs,
        Instant createdAt) {

    public int totalTokens() {
        return (promptTokens == null ? 0 : promptTokens)
                + (completionTokens == null ? 0 : completionTokens);
    }
}
