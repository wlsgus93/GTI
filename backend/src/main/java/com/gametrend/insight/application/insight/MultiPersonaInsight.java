package com.gametrend.insight.application.insight;

import com.gametrend.insight.domain.insight.Persona;
import java.time.Instant;
import java.util.List;

/**
 * P2 AI 인사이트 — Multi-persona 응답 (W6 D2).
 *
 * <p>여러 페르소나 동시 호출. Virtual Threads로 병렬화 (W3 D4 P3 비교 패턴 재사용).
 * 각 페르소나는 독립 fallback chain (Redis → DB → LLM → stale).
 *
 * <p>응답 JSON 예 (`?personas=INDIE,INVESTOR`):
 * <pre>
 * {
 *   "gameId": 1,
 *   "perspectives": [
 *     {"persona": "INDIE", "personaLabel": "인디 개발자", "summary": "...", "cached": false, ...},
 *     {"persona": "INVESTOR", "personaLabel": "투자자", "summary": "...", "cached": true, ...}
 *   ],
 *   "totalLatencyMs": 25,    // 병렬 호출 wall-clock — 4 페르소나 ≈ max 1 페르소나
 *   "respondedAt": "..."
 * }
 * </pre>
 *
 * @param totalLatencyMs 서버 측 병렬 호출 wall-clock (정량 지표 — Virtual Threads 효과 client에서도 검증)
 */
public record MultiPersonaInsight(
        Long gameId,
        List<Perspective> perspectives,
        long totalLatencyMs,
        Instant respondedAt) {

    /**
     * 페르소나별 응답 항목.
     *
     * <p>기존 {@link GameInsight} 정보 + 페르소나 메타.
     */
    public record Perspective(
            Persona persona,
            String personaLabel,
            String summary,
            int totalTokens,
            String model,
            String promptVersion,
            boolean cached,
            boolean stale,
            Instant generatedAt,
            Instant expiresAt) {

        /** {@link GameInsight} → {@link Perspective} 변환. */
        public static Perspective from(GameInsight gi, Persona persona) {
            return new Perspective(
                    persona,
                    persona.label(),
                    gi.summary(),
                    gi.totalTokens(),
                    gi.model(),
                    gi.promptVersion(),
                    gi.cached(),
                    gi.stale(),
                    gi.generatedAt(),
                    gi.expiresAt());
        }
    }
}
