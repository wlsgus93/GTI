package com.gametrend.insight.infrastructure.external.steamspy;

import com.gametrend.insight.application.port.out.SteamSpyPort;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.steamspy.dto.AppDetailsResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * SteamSpy 어댑터. 24시간 캐시로 1 req/sec 레이트 리밋 부담 완화.
 *
 * <p>Bucket4j 토큰 버킷은 Day 7 검토 (현재는 캐시로 충분). 비공식 API라 응답 정확도 낮음 — fallback 신호로만 사용.
 */
@Component
public class SteamSpyClientImpl extends AbstractExternalApiClient implements SteamSpyPort {

    private static final String SOURCE = "steamspy";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // 1 req/sec 회피용 강한 캐시

    public SteamSpyClientImpl(
            @Qualifier(SteamSpyWebClientConfig.STEAMSPY_WEB_CLIENT) WebClient webClient,
            RedisCacheTemplate cache,
            RetryPolicy retry,
            ExternalApiMetrics metrics,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        super(webClient, cache, retry, metrics, circuitBreakerRegistry);
    }

    @Override
    protected String sourceName() {
        return SOURCE;
    }

    @Override
    public Optional<SteamSpyEstimates> fetchEstimates(long gameId, long steamAppId) {
        String uri = "?request=appdetails&appid=" + steamAppId;
        String cacheKey = "ext:steamspy:appdetails:" + steamAppId;

        return getCached(uri, AppDetailsResponse.class, cacheKey, CACHE_TTL)
                .map(r -> new SteamSpyEstimates(
                        r.appId(), r.name(), r.owners(), r.averageForever(), r.median2weeks(), r.ccu()));
    }
}
