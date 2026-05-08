package com.gametrend.insight.application.dimension.community;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * D5 커뮤니티 활성도 차원 (W6 D3 — 7차원 3/7).
 *
 * <p>인디 페르소나 핵심 — Pain Point 추출 + 활성도 점수.
 *
 * <p>입력: mention_snapshot (Reddit + YouTube — 키워드 매칭 기반 estimate).
 *
 * <p>응답 구조 (룰 §4 데이터 신뢰 등급):
 * <ul>
 *   <li>Estimate 등급 데이터 — 절대값 비교 X, 상대 (다른 게임 대비) 비교만 의미
 *   <li>활성도 Z-score: 전체 게임 평균 대비 표준편차 단위
 * </ul>
 *
 * @param activityZScore  전체 게임 mention 평균 대비 Z-score (양수 = 활발 / 음수 = 조용함)
 * @param activityClass   "VERY_ACTIVE" / "ACTIVE" / "NORMAL" / "QUIET" / "VERY_QUIET" (Z-score ±1.0σ 기준)
 * @param painPoints      LLM Sentiment 분석 — V1은 빈 리스트 (W7+ 텍스트 ingestion 후)
 */
public record CommunityDimension(
        Long gameId,
        String gameName,
        long totalMentions,
        Map<SnapshotSource, Long> mentionsByPlatform,
        SentimentBreakdown sentiment,
        Double activityZScore,
        String activityClass,
        List<PainPoint> painPoints,
        Confidence confidence,
        Instant generatedAt) {

    /**
     * sentiment 분포 (POS/NEU/NEG 카운트).
     *
     * @param positiveRatio pos / (pos+neg) — neutral 제외. 0.5 미만이면 부정 우세
     */
    public record SentimentBreakdown(
            long positive,
            long neutral,
            long negative,
            Double positiveRatio) {

        public static SentimentBreakdown of(long pos, long neu, long neg) {
            Double ratio = (pos + neg == 0) ? null : Math.round((double) pos / (pos + neg) * 1000.0) / 1000.0;
            return new SentimentBreakdown(pos, neu, neg, ratio);
        }
    }

    /**
     * Pain Point — LLM Sentiment + NLP 키워드 추출 결과 (W7+ 후속).
     *
     * <p>현재 V1은 빈 리스트. mention 텍스트 컬럼 추가 + LLM Sentiment 호출 통합 후 채워짐.
     *
     * @param topic       Pain Point 주제 (예: "조작감", "매칭 시스템")
     * @param description 한국어 한 줄 요약
     * @param mentionCount 해당 Pain Point 언급 횟수
     * @param sentiment   POSITIVE / NEUTRAL / NEGATIVE
     */
    public record PainPoint(
            String topic,
            String description,
            int mentionCount,
            String sentiment) {}
}
