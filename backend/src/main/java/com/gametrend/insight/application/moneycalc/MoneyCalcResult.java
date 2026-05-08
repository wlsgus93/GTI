package com.gametrend.insight.application.moneycalc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 매출 시뮬레이션 응답 — 3 결정론적 시나리오 + Monte Carlo + 민감도.
 *
 * <p>모든 금액은 USD (cents가 아닌 BigDecimal로 노출 — UI 표시 친화적).
 *
 * @param pessimistic       비관 시나리오 결과
 * @param realistic         보통 시나리오 결과
 * @param optimistic        낙관 시나리오 결과
 * @param monteCarlo        1000회 시뮬레이션 통계 (p10/p50/p90/mean/profitProbability%)
 * @param sensitivity       민감도 — 변수별 net 영향 비율 (높을수록 영향 큼)
 * @param assumptions       사용된 상수들 (Steam cut 30%, refund 5% 등) — 투명성
 * @param generatedAt       응답 시각
 */
public record MoneyCalcResult(
        ScenarioOutput pessimistic,
        ScenarioOutput realistic,
        ScenarioOutput optimistic,
        MonteCarloOutput monteCarlo,
        List<SensitivityImpact> sensitivity,
        Assumptions assumptions,
        Instant generatedAt) {

    /**
     * @param grossRevenueUsd     소비자 결제 매출 (Steam 표시)
     * @param afterRefundUsd      gross × 0.95 (환불 ~5% 제외)
     * @param developerNetUsd     afterRefund × 0.70 (Steam 30% cut)
     * @param totalCostUsd        개발 + 마케팅 비용
     * @param profitUsd           net - totalCost
     * @param roiPct              profit / totalCost × 100 (%, null = totalCost 0)
     * @param bepUnits            손익분기 판매 단위 (totalCost 회수 위해 필요한 단위)
     */
    public record ScenarioOutput(
            BigDecimal grossRevenueUsd,
            BigDecimal afterRefundUsd,
            BigDecimal developerNetUsd,
            BigDecimal totalCostUsd,
            BigDecimal profitUsd,
            Double roiPct,
            Long bepUnits) {}

    /**
     * @param iterations            실행 시뮬레이션 횟수
     * @param netRevenueP10         net 10 percentile (90% 이 값 이상)
     * @param netRevenueP50         중앙값
     * @param netRevenueP90         90 percentile (10% 이 값 이상)
     * @param netRevenueMean        평균
     * @param netRevenueStdDev      표준편차
     * @param profitProbabilityPct  profit > 0 비율 (의사결정 핵심 지표)
     */
    public record MonteCarloOutput(
            int iterations,
            BigDecimal netRevenueP10,
            BigDecimal netRevenueP50,
            BigDecimal netRevenueP90,
            BigDecimal netRevenueMean,
            BigDecimal netRevenueStdDev,
            Double profitProbabilityPct) {}

    /**
     * 민감도 — 변수를 비관~낙관으로 변동시켰을 때 net 영향.
     *
     * @param variable      'owners' 또는 'priceCents'
     * @param pessimisticNetUsd  변수만 비관, 나머지 보통일 때 net
     * @param optimisticNetUsd   변수만 낙관, 나머지 보통일 때 net
     * @param impactRatio   |opt - pess| / |realistic_net| (1.0 = realistic 한도, 큰 값 = 더 민감)
     */
    public record SensitivityImpact(
            String variable,
            BigDecimal pessimisticNetUsd,
            BigDecimal optimisticNetUsd,
            Double impactRatio) {}

    /** 사용된 상수 — 투명성 + 모델 검증용. */
    public record Assumptions(
            double refundRate,
            double steamCut,
            int monteCarloDistribution,  // 0=triangular, 1=PERT (W4 후속)
            long randomSeed) {}
}
