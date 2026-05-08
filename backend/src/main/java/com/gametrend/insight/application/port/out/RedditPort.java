package com.gametrend.insight.application.port.out;

import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import java.util.Optional;

/**
 * Reddit OAuth API 포트 — 게임 멘션 카운트.
 */
public interface RedditPort {

    /**
     * 게임 이름으로 Reddit 검색 → 검색 결과 수를 MentionSnapshot으로 반환.
     */
    Optional<MentionSnapshot> fetchMentionCount(long gameId, String gameName);
}
