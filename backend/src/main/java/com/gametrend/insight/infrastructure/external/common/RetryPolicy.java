package com.gametrend.insight.infrastructure.external.common;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

/**
 * 외부 API 호출 재시도 정책. 5xx / 429 / timeout / IOException만 재시도.
 *
 * <p>4xx (rate limit 제외)는 영구 에러로 간주, 재시도하지 않음.
 */
@Component
public class RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(RetryPolicy.class);

    private final long maxAttempts;
    private final Duration backoff;
    private final Duration timeout;

    public RetryPolicy(
            @Value("${gti.external.retry.max-attempts:3}") long maxAttempts,
            @Value("${gti.external.retry.backoff-ms:1000}") long backoffMs,
            @Value("${gti.external.timeout-ms:5000}") long timeoutMs) {
        this.maxAttempts = maxAttempts;
        this.backoff = Duration.ofMillis(backoffMs);
        this.timeout = Duration.ofMillis(timeoutMs);
    }

    public Retry spec() {
        return Retry.backoff(maxAttempts, backoff)
                .filter(this::isRetryable)
                .doBeforeRetry(rs -> log.warn(
                        "Retry attempt {} after error: {}",
                        rs.totalRetries() + 1,
                        rs.failure().getMessage()));
    }

    public Duration timeout() {
        return timeout;
    }

    private boolean isRetryable(Throwable t) {
        // AbstractExternalApiClient의 onStatus 매핑 후: 5xx → ExternalApiException.Server
        if (t instanceof ExternalApiException.Server) {
            return true;
        }
        // 4xx (RateLimit 포함) → ExternalApiException.Client/RateLimit는 즉시 실패 (재시도 X)
        // RateLimit은 Retry-After 존중하려면 별도 전략 필요 → 호출자가 처리
        // belt-and-suspenders: onStatus를 거치지 않는 경로 대비
        if (t instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            return status >= 500;
        }
        // 네트워크 레벨 에러
        return t instanceof TimeoutException || t instanceof IOException;
    }
}
