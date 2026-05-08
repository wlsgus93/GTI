package com.gametrend.insight.infrastructure.external.opencritic;

import com.gametrend.insight.application.port.out.OpenCriticPort;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.opencritic.dto.GameScoreResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpenCriticClientImpl extends AbstractExternalApiClient implements OpenCriticPort {

    private static final String SOURCE = "opencritic";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // 평론 점수 변화는 느림

    public OpenCriticClientImpl(
            @Qualifier(OpenCriticWebClientConfig.OPENCRITIC_WEB_CLIENT) WebClient webClient,
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
    public Optional<OpenCriticScore> fetchScore(long gameId, long openCriticGameId) {
        // baseUrl이 .../api로 끝나므로 path는 /game/{id}
        String uri = "/game/" + openCriticGameId;
        String cacheKey = "ext:opencritic:score:" + openCriticGameId;

        return getCached(uri, GameScoreResponse.class, cacheKey, CACHE_TTL).map(r -> new OpenCriticScore(
                r.id(), r.name(), r.topCriticScore(), r.tier(), r.numReviews()));
    }
}
