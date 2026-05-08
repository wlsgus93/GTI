package com.gametrend.insight.application.agent;

import com.gametrend.insight.application.agent.IntentClassifier.Intent;
import com.gametrend.insight.application.agent.IntentClassifier.Topic;

/**
 * Agent 응답 — Layer 1 분류 결과 + Layer 3 cloud LLM 응답 (또는 hardcoded).
 *
 * @param sessionId         채팅 세션 ID
 * @param messageId         저장된 assistant 메시지 ID
 * @param content           응답 본문
 * @param topic             Layer 1 분류 — topic
 * @param intent            Layer 1 분류 — intent
 * @param classifierBlocked true = cloud 호출 X (hardcoded 응답) / false = cloud 호출됨
 * @param model             사용된 cloud 모델 ID (블락된 경우 null)
 * @param promptTokens      cloud 호출 시 input tokens
 * @param completionTokens  cloud 호출 시 output tokens
 * @param cached            L1/L2 캐시 hit 여부
 * @param latencyMs         전체 latency (분류 + 컨텍스트 + cloud)
 */
public record AgentResponse(
        Long sessionId,
        Long messageId,
        String content,
        Topic topic,
        Intent intent,
        boolean classifierBlocked,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        boolean cached,
        long latencyMs) {

    public static AgentResponse blocked(Long sessionId, Long messageId, String content,
            Topic topic, Intent intent, long latencyMs) {
        return new AgentResponse(sessionId, messageId, content, topic, intent, true,
                null, 0, 0, false, latencyMs);
    }
}
