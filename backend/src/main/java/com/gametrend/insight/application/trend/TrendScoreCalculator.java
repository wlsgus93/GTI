package com.gametrend.insight.application.trend;

import org.springframework.stereotype.Component;

/**
 * D2 흥행 게임 차원의 TrendScore 산출 — v1 (W2 Day 1).
 *
 * <p>v1 공식: 단순 정규화. CCU만 활용 (정식 공식은 W2 후반에 도입):
 * <pre>{@code
 * score = log10(ccu + 1) / log10(maxCcu + 1) * 100
 * }</pre>
 *
 * <p>왜 v1 단순:
 * <ul>
 *   <li>W2 Day 1엔 시계열 데이터가 부족 (시드 2 시점만)
 *   <li>실 공식 (CCU 변화율 + Twitch + Reddit + 평점 가중합)은 데이터 누적 후 적용
 *   <li>프론트 표시 (TrendBoardPage) 즉시 가능
 * </ul>
 *
 * <p>정식 공식 (`docs/analysis-dimensions.md`):
 * <pre>{@code
 * TrendScore = w1*ccu_growth + w2*wishlist_growth + w3*twitch_growth
 *            + w4*youtube_growth + w5*reddit_growth + w6*review_score
 * }</pre>
 */
@Component
public class TrendScoreCalculator {

    /**
     * @param ccu     현재 CCU
     * @param maxCcu  데이터셋 내 최댓값 (정규화 기준)
     * @return 0~100 점수
     */
    public double calculate(long ccu, long maxCcu) {
        if (maxCcu <= 0 || ccu <= 0) {
            return 0.0;
        }
        double score = Math.log10(ccu + 1.0) / Math.log10(maxCcu + 1.0) * 100.0;
        return clamp(score, 0.0, 100.0);
    }

    /**
     * 두 시점 CCU 비교 → 변화율 (%).
     *
     * @return 변화율 (예: +5.0 → 5% 증가). previous=0이면 null
     */
    public Double calculateDeltaPct(long current, long previous) {
        if (previous <= 0) {
            return null;
        }
        return ((double) (current - previous) / previous) * 100.0;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
