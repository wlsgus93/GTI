package com.gametrend.insight.application.verification;

import com.gametrend.insight.application.economics.Confidence;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * 캠페인 영향 분석 — 시차 상관 (W7 D2). 마케터 페르소나 핵심.
 *
 * <p>"캠페인 t+lag일 후 CCU에 영향" — 시차 상관 (Pearson + lag window).
 *
 * <p>한계 정직 (룰 safety.md):
 * <ul>
 *   <li>**상관 ≠ 인과** — 다른 요인 가능 (계절성 / 동시 이벤트 / Steam 세일)
 *   <li>Pearson 선형 가정
 *   <li>Sample 부족 시 null (n &lt; 5+lag → MIN_SAMPLE_SIZE 미달)
 *   <li>spurious correlation 위험 (짧은 시계열)
 * </ul>
 *
 * @param bestLagDays         최적 lag (일) — null = 분석 불가
 * @param bestCorrelation     해당 lag에서 r (-1~+1) — null = 분석 불가
 * @param correlationsByLag   모든 lag별 r
 * @param sampleSize          분석에 사용된 일별 sample 수
 * @param interpretation      자연어 해석 (마케터 페르소나용)
 * @param confidence          sample size + 분포 기반 신뢰도
 */
public record CampaignImpactAnalysis(
        Long campaignId,
        Long gameId,
        String campaignName,
        Integer bestLagDays,
        Double bestCorrelation,
        Map<Integer, Double> correlationsByLag,
        int sampleSize,
        LocalDate fromDate,
        LocalDate toDate,
        String interpretation,
        Confidence confidence,
        Instant analyzedAt) {

    public boolean hasResult() {
        return bestLagDays != null;
    }
}
