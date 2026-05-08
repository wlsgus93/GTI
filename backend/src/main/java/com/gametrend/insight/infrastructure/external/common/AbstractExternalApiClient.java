package com.gametrend.insight.infrastructure.external.common;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * 모든 외부 API 클라이언트의 베이스. 9개 어댑터가 공유.
 *
 * <p>제공:
 * <ul>
 *   <li>{@link #getCached} — GET + 캐시 우선 + 재시도 + 메트릭 + 회로 차단기
 *   <li>{@link #postCachedText} — POST (text body) + 동일 패턴 (IGDB Apicalypse용)
 *   <li>HTTP 상태별 도메인 예외 매핑 (4xx → Client, 5xx → Server, 429 → RateLimit)
 *   <li>Resilience4j {@link CircuitBreaker} per-source — 5 연속 실패 → OPEN, 30s 후 HALF_OPEN
 *   <li>Virtual Thread 안전한 imperative API ({@code .block()} 사용 OK)
 * </ul>
 *
 * <p>Day 6에서 회로 차단기 추가 — 한 소스가 지속 장애일 때 다른 소스를 차단하지 않도록 격리.
 */
public abstract class AbstractExternalApiClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractExternalApiClient.class);

    protected final WebClient webClient;
    protected final RedisCacheTemplate cache;
    protected final RetryPolicy retry;
    protected final ExternalApiMetrics metrics;
    protected final CircuitBreakerRegistry circuitBreakerRegistry;

    protected AbstractExternalApiClient(
            WebClient webClient,
            RedisCacheTemplate cache,
            RetryPolicy retry,
            ExternalApiMetrics metrics,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.webClient = webClient;
        this.cache = cache;
        this.retry = retry;
        this.metrics = metrics;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /** 데이터 소스 식별자 (메트릭/캐시 키/로그 태그/회로 차단기 이름). */
    protected abstract String sourceName();

    /**
     * 캐시 우선 GET. 캐시 hit → 즉시 반환, miss → WebClient GET + 캐시 적재.
     */
    protected <T> Optional<T> getCached(String uri, Class<T> type, String cacheKey, Duration ttl) {
        return executeWithCache(() -> webClient.get().uri(uri).retrieve(), type, cacheKey, ttl);
    }

    /**
     * 캐시 우선 POST (text body). IGDB Apicalypse 등 POST + 텍스트 본문 API용.
     */
    protected <T> Optional<T> postCachedText(
            String uri, String textBody, Class<T> type, String cacheKey, Duration ttl) {
        return executeWithCache(
                () -> webClient
                        .post()
                        .uri(uri)
                        .contentType(MediaType.TEXT_PLAIN)
                        .bodyValue(textBody)
                        .retrieve(),
                type,
                cacheKey,
                ttl);
    }

    /**
     * 공통 캐시 + 재시도 + 메트릭 + 에러 매핑 + 회로 차단기 파이프라인.
     */
    private <T> Optional<T> executeWithCache(
            java.util.function.Supplier<WebClient.ResponseSpec> responseSpec,
            Class<T> type,
            String cacheKey,
            Duration ttl) {
        Optional<T> cached = cache.get(cacheKey, type);
        if (cached.isPresent()) {
            metrics.incrementCacheHit(sourceName());
            return cached;
        }
        metrics.incrementCacheMiss(sourceName());

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker(sourceName());
        Timer.Sample sample = metrics.startTimer();
        try {
            T body = responseSpec
                    .get()
                    .onStatus(s -> s.is4xxClientError(), this::map4xx)
                    .onStatus(s -> s.is5xxServerError(), this::map5xx)
                    .bodyToMono(type)
                    .transformDeferred(CircuitBreakerOperator.of(breaker))
                    .timeout(retry.timeout())
                    .retryWhen(retry.spec())
                    .block();

            metrics.recordDuration(sourceName(), "success", sample);
            metrics.incrementCalls(sourceName(), 200);
            if (body != null) {
                cache.put(cacheKey, body, ttl);
            }
            return Optional.ofNullable(body);
        } catch (ExternalApiException e) {
            metrics.recordDuration(sourceName(), "error", sample);
            throw e;
        } catch (Exception e) {
            metrics.recordDuration(sourceName(), "error", sample);
            log.error("External API call failed source={}", sourceName(), e);
            throw new ExternalApiException.Server(sourceName(), "Unexpected error: " + e.getMessage(), e);
        }
    }

    private Mono<? extends Throwable> map4xx(ClientResponse response) {
        int status = response.statusCode().value();
        metrics.incrementCalls(sourceName(), status);
        if (status == 429) {
            String retryAfter = response.headers().asHttpHeaders().getFirst("Retry-After");
            long retryMs = parseRetryAfterMs(retryAfter);
            return Mono.error(new ExternalApiException.RateLimit(sourceName(), retryMs, "Rate limit hit"));
        }
        return Mono.error(new ExternalApiException.Client(sourceName(), status, "Client error " + status));
    }

    private Mono<? extends Throwable> map5xx(ClientResponse response) {
        int status = response.statusCode().value();
        metrics.incrementCalls(sourceName(), status);
        return Mono.error(new ExternalApiException.Server(
                sourceName(),
                "Server error " + status,
                new WebClientResponseException(status, String.valueOf(status), null, null, null)));
    }

    private long parseRetryAfterMs(String retryAfter) {
        if (retryAfter == null) {
            return 1000L;
        }
        try {
            return Long.parseLong(retryAfter.trim()) * 1000L;
        } catch (NumberFormatException e) {
            return 1000L;
        }
    }
}
