package com.gametrend.insight.application.game;

import java.time.Instant;
import java.util.List;

/**
 * P2 게임 상세 — 플레이어 분석 탭 응답.
 *
 * <p>한 게임의 "사람" 측면 데이터를 한 번에 묶어서 제공:
 * <ul>
 *   <li>현재 동시접속자 (Steam Web)
 *   <li>리뷰 (Steam Storefront)
 *   <li>라이브 시청자 (Twitch)
 *   <li>커뮤니티 멘션 카운트 (YouTube + Reddit)
 * </ul>
 *
 * <p>실 ingestion 잡이 돌기 전엔 일부 필드가 null. 프론트는 null-safe 렌더링.
 */
public record PlayerInsight(
        Long gameId,
        PlayerStats players,
        Integer twitchViewers,
        List<MentionByPlatform> mentions,
        Instant lastUpdated) {

    /**
     * 게임 플레이어 측면 통계.
     *
     * @param concurrentPlayers       현재 CCU (없으면 null)
     * @param reviewScorePositive     긍정 리뷰 수
     * @param reviewScoreTotal        전체 리뷰 수
     * @param reviewScorePercent      긍정 비율 (% — total > 0일 때만)
     */
    public record PlayerStats(
            Integer concurrentPlayers,
            Integer reviewScorePositive,
            Integer reviewScoreTotal,
            Double reviewScorePercent) {}

    /**
     * 플랫폼별 멘션 카운트.
     *
     * @param source        소스 (예: "YOUTUBE", "REDDIT")
     * @param mentionCount  최근 스냅샷의 카운트
     * @param capturedAt    측정 시각
     */
    public record MentionByPlatform(String source, int mentionCount, Instant capturedAt) {}
}
