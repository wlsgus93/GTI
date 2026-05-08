package com.gametrend.insight.domain.dimension.factor;

import java.time.Instant;
import java.util.List;

/**
 * D6 — 흥행 요인 (Hit Factor).
 *
 * <p>입력: 게임 메타 (name, developer, description) + mention_snapshot sentiment 분포.
 * 산출: 긍/부정 요인 키워드 + 한 줄 요약.
 *
 * <p><b>V1 한계</b> (정직한 표시):
 * Steam appreviews 같은 raw 리뷰 텍스트 어댑터 부재 → 게임 메타 + sentiment 통계 기반 LLM 추론.
 * {@code sourceLabel = "ESTIMATED_FROM_METADATA"} + confidence MEDIUM.
 * V2: Steam appreviews 어댑터 추가 시 실 리뷰 텍스트 활용 (confidence HIGH).
 */
public record HitFactor(
        long gameId,
        String gameName,
        List<FactorKeyword> positiveFactors,
        List<FactorKeyword> negativeFactors,
        String summary,
        String model,
        String sourceLabel,
        Confidence confidence,
        SentimentBaseline sentimentBaseline,
        Instant generatedAt) {

    public record FactorKeyword(
            String keyword,
            String reasoning) {}

    public record SentimentBaseline(
            int totalMentions,
            double positiveRatio,
            double neutralRatio,
            double negativeRatio) {}

    public enum Confidence { HIGH, MEDIUM, LOW }
}
