package com.gametrend.insight.application.economics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * 단가/효율 지표 — 순수 함수. 광고비 부재로 결제·관심·플레이 간 전환 효율 계산.
 *
 * <p>공식:
 * <ul>
 *   <li>viewToPlayRatio       = CCU / TwitchViewers (>1.0 시청자→플레이 전환 강함)
 *   <li>mentionToPlayRatio    = mentionsTotal / CCU (>1.0 = 입소문 > 실 플레이)
 *   <li>priceEfficiency       = CCU / priceUsd (가격 1$ 당 동접자, F2P 시 null)
 *   <li>reviewCostPerPositive = priceUsd / 긍정 리뷰 1건 (F2P 또는 리뷰=0 시 null)
 * </ul>
 *
 * <p>분모 null/0 가드 모두 적용.
 */
@Component
public final class UnitEconomicsCalculator {

    /**
     * @param ccu                    동시접속자 (null이면 ratio들 null)
     * @param twitchViewers          Twitch 시청자 (null/0이면 viewToPlay null)
     * @param totalMentions          YT+Reddit mentions 합 (null이면 mentionToPlay null)
     * @param priceCents             가격 USD cents (null/0=F2P → priceEfficiency, reviewCost null)
     * @param reviewScorePositive    긍정 리뷰 수 (null/0이면 reviewCost null)
     */
    public EconomicsInsight.UnitEconomics calculate(
            Integer ccu,
            Integer twitchViewers,
            Integer totalMentions,
            Long priceCents,
            Integer reviewScorePositive) {

        Double viewToPlay = (ccu != null && twitchViewers != null && twitchViewers > 0)
                ? round3(ccu.doubleValue() / twitchViewers.doubleValue())
                : null;

        Double mentionToPlay = (ccu != null && ccu > 0 && totalMentions != null)
                ? round3(totalMentions.doubleValue() / ccu.doubleValue())
                : null;

        Double priceEfficiency = null;
        BigDecimal reviewCost = null;
        if (priceCents != null && priceCents > 0) {
            BigDecimal priceUsd = BigDecimal.valueOf(priceCents).movePointLeft(2);
            if (ccu != null) {
                priceEfficiency = round3(ccu.doubleValue() / priceUsd.doubleValue());
            }
            if (reviewScorePositive != null && reviewScorePositive > 0) {
                reviewCost = priceUsd.divide(BigDecimal.valueOf(reviewScorePositive), 4, RoundingMode.HALF_UP);
            }
        }

        return new EconomicsInsight.UnitEconomics(viewToPlay, mentionToPlay, priceEfficiency, reviewCost);
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
