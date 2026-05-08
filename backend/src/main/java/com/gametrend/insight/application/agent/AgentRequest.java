package com.gametrend.insight.application.agent;

import com.gametrend.insight.domain.insight.Persona;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 자연어 query 요청. CommandBar / AgentPanel 에서 호출.
 *
 * @param query     자연어 질문 (5~2000 char)
 * @param sessionId null = 새 세션 / 값 = 기존 세션 (꼬리질문)
 * @param persona   세션 새로 시작 시 페르소나. 기존 세션이면 무시 (세션의 페르소나 고정)
 */
public record AgentRequest(
        @NotBlank @Size(min = 1, max = 2000) String query,
        Long sessionId,
        Persona persona) {
}
