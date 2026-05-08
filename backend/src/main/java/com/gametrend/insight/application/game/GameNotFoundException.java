package com.gametrend.insight.application.game;

import com.gametrend.insight.domain.common.DomainException;

/**
 * 요청한 ID의 게임이 DB에 존재하지 않을 때.
 *
 * <p>{@code @RestControllerAdvice}는 도메인 예외를 422 (UNPROCESSABLE_ENTITY)로 매핑하지만,
 * 리소스 미존재는 의미상 404가 적합 — 컨트롤러 advice에서 별도 처리 또는 향후 별도 예외 베이스 분리.
 *
 * <p>현재는 {@link DomainException}으로 422 반환. 차후 {@code ResourceNotFoundException} 분리 검토.
 */
public class GameNotFoundException extends DomainException {

    private final long gameId;

    public GameNotFoundException(long gameId) {
        super("Game not found: id=" + gameId);
        this.gameId = gameId;
    }

    public long gameId() {
        return gameId;
    }

    @Override
    public String errorCode() {
        return "game-not-found";
    }
}
