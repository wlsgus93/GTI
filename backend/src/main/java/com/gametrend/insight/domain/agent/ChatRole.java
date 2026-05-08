package com.gametrend.insight.domain.agent;

/**
 * 채팅 메시지 역할 — DB {@code chat_message.role} 와 매핑.
 */
public enum ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
    /** Compaction 결과 — 6턴 넘으면 LLM 자체 요약으로 압축. */
    SUMMARY
}
