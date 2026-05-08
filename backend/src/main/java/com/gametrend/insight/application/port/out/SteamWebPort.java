package com.gametrend.insight.application.port.out;

import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import java.util.Optional;

/**
 * Steam Web API에서 게임의 동시접속자 수를 조회하는 포트.
 *
 * <p>구현체: {@link com.gametrend.insight.infrastructure.external.steam.SteamWebClientImpl}.
 *
 * <p>호출자(application 서비스 또는 오케스트레이터)는 GTI DB의 {@code gameId}와
 * Steam의 {@code appId}를 둘 다 알고 있어야 한다 (게임 마스터에서 매핑).
 */
public interface SteamWebPort {

    /**
     * 특정 게임의 현재 동시접속자 수를 PlayerSnapshot 형태로 반환.
     *
     * @param gameId GTI DB의 게임 PK (스냅샷에 들어갈 값)
     * @param appId  Steam appid
     * @return Steam 응답이 성공이면 PlayerSnapshot, 실패(result != 1) 또는 응답 없음이면 empty
     */
    Optional<PlayerSnapshot> fetchCurrentPlayers(long gameId, long appId);
}
