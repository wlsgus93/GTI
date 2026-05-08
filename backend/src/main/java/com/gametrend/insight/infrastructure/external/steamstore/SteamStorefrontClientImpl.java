package com.gametrend.insight.infrastructure.external.steamstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.gametrend.insight.application.port.out.SteamStorefrontPort;
import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Steam Storefront 어댑터 — 패턴 재사용 검증 사례.
 *
 * <p>Day 3의 SteamWebClientImpl과 비교하면, 본질적으로 다음만 다름:
 * <ul>
 *   <li>WebClient (Storefront URL 사용)
 *   <li>sourceName 반환값 ({@code "steam_store"})
 *   <li>URI/캐시 키/매퍼
 * </ul>
 * <p>그 외 재시도, 캐싱, 메트릭, 에러 매핑은 모두 {@link AbstractExternalApiClient}가 처리.
 */
@Component
public class SteamStorefrontClientImpl extends AbstractExternalApiClient implements SteamStorefrontPort {

    private static final String SOURCE = "steam_store";
    private static final Duration CACHE_TTL = Duration.ofHours(1); // 가격은 1시간 캐시

    public SteamStorefrontClientImpl(
            @Qualifier("steamStorefrontWebClient") WebClient webClient,
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
    public Optional<PriceSnapshot> fetchPrice(long gameId, long appId) {
        String uri = "/appdetails?appids=" + appId + "&cc=us";
        String cacheKey = "ext:steamstore:price:" + appId;

        return getCached(uri, JsonNode.class, cacheKey, CACHE_TTL)
                .flatMap(node -> SteamStorefrontMapper.toPriceSnapshot(node, appId, gameId));
    }
}
