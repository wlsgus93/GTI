package com.gametrend.insight.application.trend;

/**
 * P1 트렌드 보드의 게임 카드 1개 데이터.
 *
 * <p>프론트 `MockGame` (frontend/src/lib/mock/games.ts)와 매핑되도록 설계:
 * <pre>{@code
 *   { id, title, genre, platform, trendScore, ccuDeltaPct }
 * }</pre>
 *
 * @param id            Steam appid (string으로 직렬화 — JS Number 정밀도 회피)
 * @param title         게임 명
 * @param genre         대표 장르 (W2 v1: placeholder; W3에 D4 클러스터링 결과로 교체)
 * @param platform      플랫폼 (W2 v1: "Steam" 고정; 모바일 등은 W3+)
 * @param trendScore    0~100 점수 (소수 1자리)
 * @param ccuDeltaPct   24h CCU 변화율 (%, 소수 1자리; 이전 데이터 없으면 null)
 * @param concurrentPlayers 현재 CCU (참고용, 프론트 표시 X)
 */
public record TrendBoardItem(
        String id,
        String title,
        String genre,
        String platform,
        double trendScore,
        Double ccuDeltaPct,
        int concurrentPlayers) {
}
