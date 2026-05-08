package com.gametrend.insight.application.insight;

import java.math.BigDecimal;
import java.util.List;

/**
 * 프롬프트 조립 입력 — 1분 요약을 위한 게임 컨텍스트 스냅샷.
 *
 * <p>모든 필드 nullable — 부분 데이터로도 의미 있는 요약 생성.
 *
 * @param gameName             게임명 (필수)
 * @param genres               장르 리스트 (nullable)
 * @param developer            개발사 (nullable)
 * @param latestCcu            최신 CCU
 * @param ccuDeltaPct          24h CCU 변화율 (%)
 * @param twitchViewers        최신 Twitch 시청자
 * @param totalMentions        YT+Reddit mentions 합
 * @param reviewScorePercent   긍정 리뷰 비율 (%)
 * @param ownersMid            SteamSpy owners 중간값
 * @param priceUsd             가격 (USD)
 * @param developerNetRevenue  추정 개발사 순매출 (USD)
 * @param viewToPlayRatio      CCU/Twitch ratio
 * @param confidence           Economics 신뢰도 (HIGH/MEDIUM/LOW)
 */
public record InsightContext(
        String gameName,
        List<String> genres,
        String developer,
        Integer latestCcu,
        Double ccuDeltaPct,
        Integer twitchViewers,
        Integer totalMentions,
        Double reviewScorePercent,
        Long ownersMid,
        BigDecimal priceUsd,
        BigDecimal developerNetRevenue,
        Double viewToPlayRatio,
        String confidence) {
}
