package com.gametrend.insight.application.insight;

import java.time.Instant;

/**
 * P2 AI 인사이트 탭 응답.
 *
 * @param summary        한국어 요약 본문 (LLM 생성)
 * @param promptVersion  프롬프트 템플릿 버전 (디버그/감사용)
 * @param totalTokens    prompt + completion 합 (운영 비용 추적)
 * @param model          LLM 모델 식별자
 * @param cached         캐시 hit 여부 (Redis 또는 DB fresh — true=기존, false=신규 LLM 호출)
 * @param stale          stale fallback 여부 (true=LLM 장애로 TTL 만료된 옛 분석 반환). W3 D1 도입.
 * @param generatedAt    분석 생성 시점 (cache hit 시 원본 시점)
 * @param expiresAt      캐시 만료 시점 (stale=true면 이미 과거)
 */
public record GameInsight(
        Long gameId,
        String summary,
        String promptVersion,
        int totalTokens,
        String model,
        boolean cached,
        boolean stale,
        Instant generatedAt,
        Instant expiresAt) {
}
