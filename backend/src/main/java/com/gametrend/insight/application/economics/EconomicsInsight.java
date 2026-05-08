package com.gametrend.insight.application.economics;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * P2 게임 상세 — 매출/단가 탭 응답 (W2 Day 4).
 *
 * <p>입력 데이터가 부분적이라도 응답이 나가도록 모든 nested 필드 nullable.
 *
 * @param revenue       매출 추정 (owners + 가격 둘 다 있어야 채워짐, 그 외 null)
 * @param unitEconomics 단가/효율 지표 (분모 null/0인 항목만 null)
 * @param confidence    매출 추정 신뢰도
 * @param lastUpdated   사용된 스냅샷 중 가장 최근 시점
 */
public record EconomicsInsight(
        Long gameId,
        RevenueEstimate revenue,
        UnitEconomics unitEconomics,
        Confidence confidence,
        Instant lastUpdated) {

    /**
     * SteamSpy owners 범위 + Steam Storefront 가격 → 추정 매출.
     *
     * @param ownersLow              SteamSpy owners 하한
     * @param ownersHigh             SteamSpy owners 상한
     * @param ownersMid              (low+high)/2
     * @param priceUsd               단위 USD (소수 2자리)
     * @param grossLifetimeRevenue   ownersMid × price (Steam 표시 매출)
     * @param afterRefundRevenue     gross × 0.95 (Valve 환불률 ~5%)
     * @param developerNet           afterRefund × 0.70 (Steam 30% cut, 보수적)
     * @param estimatedDau           CCU peak × 8 (산업 관례)
     * @param estimatedMau           DAU × 3.5 (stickiness)
     */
    public record RevenueEstimate(
            Long ownersLow,
            Long ownersHigh,
            Long ownersMid,
            BigDecimal priceUsd,
            BigDecimal grossLifetimeRevenue,
            BigDecimal afterRefundRevenue,
            BigDecimal developerNet,
            Integer estimatedDau,
            Integer estimatedMau) {}

    /**
     * 단가/효율 지표 — 광고비 데이터 없으므로 파생.
     *
     * @param viewToPlayRatio       CCU / Twitch 시청자 (>1.0 = 시청자→플레이 전환 강함)
     * @param mentionToPlayRatio    (YT+Reddit mentions) / CCU (>1.0 = 입소문 강세)
     * @param priceEfficiency       CCU / price USD (가격 1$ 당 동접자, F2P 시 null)
     * @param reviewCostPerPositive price / 긍정 리뷰 1건당 결제 단가 (F2P 시 null)
     */
    public record UnitEconomics(
            Double viewToPlayRatio,
            Double mentionToPlayRatio,
            Double priceEfficiency,
            BigDecimal reviewCostPerPositive) {}
}
