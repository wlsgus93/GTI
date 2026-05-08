package com.gametrend.insight.domain.insight;

import java.time.Instant;

/**
 * 영속화된 AI 분석 결과. LLM 호출 결과를 캐시 + 감사용으로 보관.
 *
 * @param promptVersion 프롬프트 템플릿 버전 (INSIGHT_V1 등) — 버전 바뀌면 캐시 무효
 * @param totalTokens   prompt + completion 합 (비용 추적)
 * @param model         LLM 모델 식별자 (claude-opus-4-5 / stub / ...)
 * @param expiresAt     TTL 만료 시점 (이후 새 호출)
 */
public record Analysis(
        Long id,
        Long gameId,
        AnalysisKind kind,
        String promptVersion,
        String content,
        int promptTokens,
        int completionTokens,
        int totalTokens,
        String model,
        Instant createdAt,
        Instant expiresAt) {

    public boolean isFresh(Instant now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }
}
