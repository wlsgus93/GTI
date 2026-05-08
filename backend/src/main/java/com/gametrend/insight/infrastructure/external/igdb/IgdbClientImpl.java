package com.gametrend.insight.infrastructure.external.igdb;

import com.gametrend.insight.application.port.out.IgdbPort;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.igdb.dto.IgdbGameDto;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * IGDB 어댑터 — POST + Apicalypse 쿼리 사례.
 *
 * <p>POST가 필요하므로 {@link AbstractExternalApiClient#postCachedText}를 처음 사용.
 * 본문은 Apicalypse 문법 (텍스트). Twitch OAuth 토큰 공유.
 */
@Component
public class IgdbClientImpl extends AbstractExternalApiClient implements IgdbPort {

    private static final String SOURCE = "igdb";
    private static final Duration CACHE_TTL = Duration.ofHours(24); // 메타데이터는 거의 안 변함

    public IgdbClientImpl(
            @Qualifier(IgdbWebClientConfig.IGDB_WEB_CLIENT) WebClient webClient,
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
    public Optional<IgdbGameMeta> fetchGameMetadata(long igdbId) {
        String body = "fields name,summary,first_release_date,genres.name,cover.url; where id = " + igdbId + ";";
        String cacheKey = "ext:igdb:game:" + igdbId;

        return postCachedText("/games", body, IgdbGameDto[].class, cacheKey, CACHE_TTL)
                .flatMap(IgdbMapper::toDomain);
    }
}
