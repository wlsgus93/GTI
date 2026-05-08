package com.gametrend.insight.application.insight;

/**
 * LLM 호출 실패 + stale fallback 가능한 분석 이력 없음 → 503.
 *
 * <p>발생 조건:
 * <ol>
 *   <li>Redis hot cache miss
 *   <li>DB fresh (TTL 24h 내) miss
 *   <li>LLM 호출 실패 (또는 회로 차단기 OPEN)
 *   <li>DB stale 이력도 없음 (이 게임 한 번도 분석된 적 없음)
 * </ol>
 *
 * <p>이 경우 사용자에게 503 + retry-after 안내.
 */
public class LlmUnavailableException extends RuntimeException {

    private final long gameId;

    public LlmUnavailableException(long gameId, Throwable cause) {
        super("LLM unavailable and no cached analysis for gameId=" + gameId, cause);
        this.gameId = gameId;
    }

    public long getGameId() {
        return gameId;
    }
}
