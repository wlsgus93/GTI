package com.gametrend.insight.infrastructure.external.common;

/**
 * 외부 API 호출 실패 시 던지는 예외 베이스.
 *
 * <p>9개 데이터 소스 (Steam, IGDB, Twitch 등)와 AI/Vision/STT 클라이언트가 사용.
 * {@code @RestControllerAdvice}에서 503 Service Unavailable 또는 502 Bad Gateway로 매핑.
 */
public abstract class ExternalApiException extends RuntimeException {

    private final String source;

    protected ExternalApiException(String source, String message) {
        super(message);
        this.source = source;
    }

    protected ExternalApiException(String source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public String source() {
        return source;
    }

    /** 4xx 클라이언트 에러 (재시도 X). */
    public static final class Client extends ExternalApiException {
        private final int status;

        public Client(String source, int status, String message) {
            super(source, message);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    /** 5xx 서버 에러 또는 timeout (재시도 적용 후 실패). */
    public static final class Server extends ExternalApiException {
        public Server(String source, String message, Throwable cause) {
            super(source, message, cause);
        }
    }

    /** 429 Rate Limit (Retry-After 헤더 존중 후에도 실패). */
    public static final class RateLimit extends ExternalApiException {
        private final long retryAfterMs;

        public RateLimit(String source, long retryAfterMs, String message) {
            super(source, message);
            this.retryAfterMs = retryAfterMs;
        }

        public long retryAfterMs() {
            return retryAfterMs;
        }
    }
}
