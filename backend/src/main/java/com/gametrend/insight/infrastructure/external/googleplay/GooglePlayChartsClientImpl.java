package com.gametrend.insight.infrastructure.external.googleplay;

import com.gametrend.insight.application.port.out.GooglePlayChartsPort;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.googleplay.dto.GooglePlayChartsResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Google Play Top Charts 어댑터.
 *
 * <p>Spring 본체에서 직접 google-play-scraper 호출하지 않고, 별도 Node.js 마이크로서비스
 * (crawler-service) 의 REST endpoint 호출. ToS / Cloudflare 위험 격리 + Java 라이브러리 부재 회피.
 *
 * <p>per-game 데이터가 아니라 <b>카테고리/국가별 일괄 조회</b>. 1시간 캐시.
 */
@Component
public class GooglePlayChartsClientImpl extends AbstractExternalApiClient
        implements GooglePlayChartsPort {

    private static final String SOURCE = "google_play_charts";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    public GooglePlayChartsClientImpl(
            @Qualifier(GooglePlayWebClientConfig.GOOGLE_PLAY_WEB_CLIENT) WebClient webClient,
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
    public Optional<List<TopAppEntry>> fetchTopFreeGames(String country, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        String uri = "/charts/google-play?country=" + country + "&limit=" + safeLimit
                + "&collection=TOP_FREE";
        String cacheKey = "ext:gplay:topfree:" + country + ":" + safeLimit;

        return getCached(uri, GooglePlayChartsResponse.class, cacheKey, CACHE_TTL).map(resp -> {
            if (resp.items() == null) {
                return List.<TopAppEntry>of();
            }
            List<TopAppEntry> entries = new ArrayList<>(resp.items().size());
            for (GooglePlayChartsResponse.Item it : resp.items()) {
                entries.add(new TopAppEntry(
                        it.appId(),
                        it.title(),
                        it.developer(),
                        it.score(),
                        it.rank()));
            }
            return entries;
        });
    }
}
