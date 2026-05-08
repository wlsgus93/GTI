package com.gametrend.insight.application.economics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * 매출 추정 — 순수 함수. 광고비/실 매출 데이터 없으므로 SteamSpy owners + Steam 가격 기반.
 *
 * <p>공식:
 * <pre>
 *   ownersMid           = (low + high) / 2
 *   gross               = ownersMid × priceUsd
 *   afterRefund         = gross × (1 - REFUND_RATE)
 *   developerNet        = afterRefund × (1 - STEAM_CUT)
 *   estDau              = ccuPeak × DAU_MULTIPLIER
 *   estMau              = estDau × MAU_STICKINESS
 * </pre>
 *
 * <p>한계:
 * <ul>
 *   <li>Steam 30% → $10M 초과 시 25%, $50M 초과 시 20%로 단계 — 보수적으로 일률 30% 적용
 *   <li>지역별 가격차/할인 무시 (가장 최근 단일 priceCents)
 *   <li>F2P (price=0) → revenue 모두 0 (적절한 처리)
 * </ul>
 */
@Component
public final class RevenueEstimator {

    /** Valve 공개 환불률 ~5% (2017 Q&A). */
    public static final double REFUND_RATE = 0.05;

    /** Steam 수수료 — 첫 $10M 30% (보수적). */
    public static final double STEAM_CUT = 0.30;

    /** CCU peak → DAU 환산 배율 (산업 관례 7~10, 중간값 사용). */
    public static final double DAU_MULTIPLIER = 8.0;

    /** DAU → MAU stickiness ratio (모바일 게임 분석 관례 3~5). */
    public static final double MAU_STICKINESS = 3.5;

    /**
     * @param ownersLow  SteamSpy 하한 (null이면 estimate null 반환)
     * @param ownersHigh SteamSpy 상한
     * @param priceCents 가격 (USD cents, null 또는 0이면 매출 0 반환 — F2P)
     * @param ccuPeak    윈도우 내 최대 CCU (null이면 DAU/MAU null)
     * @return 추정치 (입력 부족 시 null)
     */
    public EconomicsInsight.RevenueEstimate estimate(
            Long ownersLow, Long ownersHigh, Long priceCents, Integer ccuPeak) {
        if (ownersLow == null || ownersHigh == null) {
            return null;
        }
        long ownersMid = (ownersLow + ownersHigh) / 2L;

        BigDecimal priceUsd = priceCents == null
                ? null
                : BigDecimal.valueOf(priceCents).movePointLeft(2);

        BigDecimal gross;
        BigDecimal afterRefund;
        BigDecimal developerNet;
        if (priceUsd == null) {
            gross = null;
            afterRefund = null;
            developerNet = null;
        } else {
            gross = priceUsd.multiply(BigDecimal.valueOf(ownersMid)).setScale(2, RoundingMode.HALF_UP);
            afterRefund = gross.multiply(BigDecimal.valueOf(1.0 - REFUND_RATE))
                    .setScale(2, RoundingMode.HALF_UP);
            developerNet = afterRefund.multiply(BigDecimal.valueOf(1.0 - STEAM_CUT))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        Integer estDau = ccuPeak == null ? null : (int) Math.round(ccuPeak * DAU_MULTIPLIER);
        Integer estMau = estDau == null ? null : (int) Math.round(estDau * MAU_STICKINESS);

        return new EconomicsInsight.RevenueEstimate(
                ownersLow, ownersHigh, ownersMid, priceUsd, gross, afterRefund, developerNet, estDau, estMau);
    }

    /**
     * 신뢰도 계산. owners 상대폭 + 가격 + 스냅샷 신선도(외부에서 판단해 stale 플래그 전달).
     *
     * @param ownersLow  하한
     * @param ownersHigh 상한
     * @param priceCents 가격 (null이면 LOW)
     * @param fresh      스냅샷이 24h 이내면 true
     */
    public Confidence assessConfidence(Long ownersLow, Long ownersHigh, Long priceCents, boolean fresh) {
        if (ownersLow == null || ownersHigh == null || priceCents == null) {
            return Confidence.LOW;
        }
        long mid = (ownersLow + ownersHigh) / 2L;
        if (mid == 0) {
            return Confidence.LOW;
        }
        double relativeWidth = (double) (ownersHigh - ownersLow) / mid;
        if (relativeWidth < 0.30 && fresh) {
            return Confidence.HIGH;
        }
        if (relativeWidth < 0.60) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }
}
