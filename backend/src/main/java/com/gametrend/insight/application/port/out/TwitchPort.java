package com.gametrend.insight.application.port.out;

import com.gametrend.insight.domain.snapshot.ViewerSnapshot;
import java.util.Optional;

/**
 * Twitch Helix 포트 — 게임 카테고리별 시청자 수 합계.
 *
 * <p>Twitch는 자체 게임 ID 체계를 사용 (Steam appid와 다름). 호출자가 매핑 알아야 함.
 */
public interface TwitchPort {

    /**
     * 특정 Twitch 게임 ID의 라이브 스트림 시청자 수 합계.
     *
     * @param gameId       GTI DB의 게임 PK
     * @param twitchGameId Twitch 카테고리 ID (예: "32982")
     * @return 시청자 합계 (스트림 0개여도 0으로 응답), API 오류면 empty
     */
    Optional<ViewerSnapshot> fetchViewers(long gameId, String twitchGameId);
}
