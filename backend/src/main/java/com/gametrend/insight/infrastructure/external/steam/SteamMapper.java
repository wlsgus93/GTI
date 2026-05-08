package com.gametrend.insight.infrastructure.external.steam;

import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.steam.dto.CurrentPlayersResponse;
import java.time.Instant;
import java.util.Optional;

/**
 * Steam 응답 DTO → 도메인 매핑.
 */
public final class SteamMapper {

    private SteamMapper() {}

    /**
     * Steam의 GetNumberOfCurrentPlayers 응답을 PlayerSnapshot으로 변환.
     *
     * @param response Steam 응답 (null 또는 result != 1이면 empty)
     * @param gameId   GTI DB의 게임 PK
     * @return 성공 응답이면 PlayerSnapshot, 아니면 empty
     */
    public static Optional<PlayerSnapshot> toPlayerSnapshot(CurrentPlayersResponse response, long gameId) {
        if (response == null
                || response.response() == null
                || response.response().result() != 1) {
            return Optional.empty();
        }
        PlayerSnapshot snapshot = new PlayerSnapshot(
                null, // id - 영속화 시 부여
                gameId,
                response.response().playerCount(),
                null, // reviewScorePositive - 별도 엔드포인트
                null, // reviewScoreTotal
                null, // ownersLow - SteamSpy
                null, // ownersHigh - SteamSpy
                Instant.now(),
                SnapshotSource.STEAM,
                false // 라이브 응답 (캐시 fallback 아님)
                );
        return Optional.of(snapshot);
    }
}
