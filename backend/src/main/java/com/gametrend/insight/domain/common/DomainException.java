package com.gametrend.insight.domain.common;

/**
 * 모든 도메인 예외의 베이스 클래스.
 *
 * <p>도메인 규칙 위반 시 던진다. {@code @RestControllerAdvice}에서
 * 422 Unprocessable Entity 등 적절한 HTTP 응답으로 매핑된다.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 클라이언트에 노출 가능한 에러 코드 (URL-safe slug).
     * 예: "game-not-found", "invalid-trend-score"
     */
    public abstract String errorCode();
}
