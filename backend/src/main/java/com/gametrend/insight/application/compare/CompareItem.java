package com.gametrend.insight.application.compare;

import java.math.BigDecimal;
import java.util.List;

/**
 * P3 게임 비교 항목 — 단일 게임의 핵심 지표 묶음.
 *
 * <p>모든 nullable 필드는 부분 데이터 graceful: viewer/mention/economics가 없어도 응답.
 *
 * @param gameId             게임 PK
 * @param steamAppId         Steam App ID (null 가능 — 모바일 등)
 * @param name               게임명
 * @param genres             장르 리스트 (raw)
 * @param coverImageUrl      커버 이미지 (radar chart 라벨에 사용)
 * @param latestCcu          최신 CCU
 * @param ccuDeltaPct        24h CCU 변화율 (%)
 * @param twitchViewers      최신 Twitch 시청자
 * @param totalMentions      YT+Reddit 멘션 합
 * @param reviewScorePercent 긍정 리뷰 비율 (%)
 * @param ownersMid          SteamSpy 추정 소유자 (중간값)
 * @param priceUsd           가격 (USD)
 * @param developerNetRevenue  추정 개발사 순매출 (USD, mid 기준)
 * @param confidence         Economics 신뢰도 (HIGH/MEDIUM/LOW, null 가능)
 */
public record CompareItem(
        Long gameId,
        Long steamAppId,
        String name,
        List<String> genres,
        String coverImageUrl,
        Integer latestCcu,
        Double ccuDeltaPct,
        Integer twitchViewers,
        Integer totalMentions,
        Double reviewScorePercent,
        Long ownersMid,
        BigDecimal priceUsd,
        BigDecimal developerNetRevenue,
        String confidence) {
}
