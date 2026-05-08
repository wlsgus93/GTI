package com.gametrend.insight.domain.agent;

import com.gametrend.insight.domain.insight.Persona;
import java.time.Instant;

/**
 * 사용자 채팅 세션 — 한 세션 = 한 페르소나 고정 (룰 95 §5.2).
 *
 * <p>provider 도 한 세션 동안 고정 (cloud LLM 일관성). 세션 변경 시 새 session 생성.
 */
public record ChatSession(
        Long id,
        Long userId,
        Persona persona,
        String title,
        Instant startedAt,
        Instant lastActiveAt,
        int totalMessages,
        int totalTokens,
        Instant closedAt) {

    public boolean isClosed() {
        return closedAt != null;
    }
}
