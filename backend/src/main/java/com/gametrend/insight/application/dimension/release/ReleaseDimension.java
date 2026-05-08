package com.gametrend.insight.application.dimension.release;

import java.time.Instant;
import java.util.List;

/**
 * D1 출시 동향 차원 — 장르별/연도별 출시 패턴 + 평균 인기도.
 *
 * <p>입력: games.release_date + genres + 최신 CCU.
 * 출력: 게임 제작자가 "어떤 장르가 최근 잘 나오나" 의사결정에 활용.
 *
 * @param byGenre     장르별 통계 (gameCount desc 정렬)
 * @param byYear      연도별 통계 (year desc, 최근부터)
 * @param totalGames  전체 분석 대상 게임 수
 * @param generatedAt 응답 생성 시각
 */
public record ReleaseDimension(
        List<GenreStats> byGenre,
        List<YearStats> byYear,
        int totalGames,
        Instant generatedAt) {

    /**
     * 장르별 집계 + Hit/Flop 분류 (W5 D1 — Z-score 기반).
     *
     * <p>Hit/Flop은 **장르 내** CCU 분포 기준 (장르 평균 ± threshold·σ).
     * 즉 "Action 장르 안에서 어떤 게임이 흥행/실패"를 알려주지 장르 간 비교 X.
     *
     * @param genre        장르명
     * @param gameCount    이 장르에 속한 게임 수
     * @param avgLatestCcu 평균 최신 CCU
     * @param maxLatestCcu 최대 CCU
     * @param topGameName  CCU 1위 게임명
     * @param hitCount     장르 내 HIT 분류 게임 수 (z > +threshold)
     * @param flopCount    장르 내 FLOP 분류 게임 수 (z < -threshold)
     * @param normalCount  장르 내 NORMAL 분류 게임 수
     */
    public record GenreStats(
            String genre,
            int gameCount,
            Integer avgLatestCcu,
            Integer maxLatestCcu,
            String topGameName,
            long hitCount,
            long flopCount,
            long normalCount) {}

    /**
     * 연도별 집계.
     *
     * @param year         출시 연도
     * @param gameCount    이 연도 출시 게임 수
     * @param avgLatestCcu 해당 연도 게임들의 평균 CCU
     */
    public record YearStats(
            int year,
            int gameCount,
            Integer avgLatestCcu) {}
}
