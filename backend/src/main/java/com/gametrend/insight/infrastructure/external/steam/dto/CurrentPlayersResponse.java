package com.gametrend.insight.infrastructure.external.steam.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Steam Web API {@code GetNumberOfCurrentPlayers/v1/} 응답.
 *
 * <p>예시 응답:
 * <pre>{@code
 * {"response": {"player_count": 12345, "result": 1}}
 * }</pre>
 *
 * <p>{@code result == 1}이면 성공, 그 외는 (앱 미존재 등) 실패.
 */
public record CurrentPlayersResponse(Response response) {

    public record Response(@JsonProperty("player_count") int playerCount, int result) {}
}
