package com.gametrend.insight.application.game;

import java.time.Instant;
import java.util.List;

/**
 * P2 게임 상세 — 동접(CCU) 시계열 응답. 차트용.
 *
 * @param gameId   GTI DB PK
 * @param range    조회 범위 코드 (예: "30d")
 * @param from     조회 시작 시각 (UTC)
 * @param to       조회 종료 시각 (UTC, 응답 생성 시점)
 * @param points   시간순 정렬된 스냅샷 (오래된 → 최신)
 */
public record CcuSeries(Long gameId, String range, Instant from, Instant to, List<Point> points) {

    /**
     * 차트의 한 점.
     *
     * @param capturedAt          스냅샷 시각 (UTC)
     * @param concurrentPlayers   동시접속자 수
     */
    public record Point(Instant capturedAt, int concurrentPlayers) {}
}
