package com.gametrend.insight.application.port.out;

import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import java.util.Optional;

/**
 * YouTube Data API v3 포트 — 게임 키워드 검색량 (멘션 카운트 근사치).
 *
 * <p>{@code search.list?type=video&q=<gameName>}의 {@code totalResults}를 mention count로 사용.
 * 정밀하진 않지만 인기도 변화 추적에는 충분.
 */
public interface YouTubePort {

    /**
     * 게임 이름으로 비디오 검색 → 검색 결과 총 개수를 MentionSnapshot으로 반환.
     *
     * @param gameId   GTI DB의 게임 PK
     * @param gameName YouTube에서 검색할 게임 이름
     */
    Optional<MentionSnapshot> fetchMentionCount(long gameId, String gameName);
}
