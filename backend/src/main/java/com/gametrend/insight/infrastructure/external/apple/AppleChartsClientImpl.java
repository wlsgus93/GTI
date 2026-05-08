package com.gametrend.insight.infrastructure.external.apple;

import com.gametrend.insight.application.port.out.AppleChartsPort;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Apple Top Charts 어댑터 — iTunes legacy RSS.
 *
 * <p>URL: {@code https://itunes.apple.com/{country}/rss/topfreeapplications/limit={N}/genre=6014/json}.
 * genre=6014 = Apple "Games" 카테고리 (server-side 필터). 1시간 캐시.
 *
 * <p>v2 spec (rss.marketingtools.apple.com/api/v2) 은 게임 카테고리 필터링 미지원
 * (응답 {@code genres:[]} 빈 배열, query param 무시) → legacy 사용. 응답 schema 가
 * Atom-style ({@code feed.entry[]} with {@code im:*} prefixed fields).
 *
 * <p>per-game 데이터가 아니라 <b>카테고리/국가별 일괄 조회</b> — 게임 마스터 발견 용도.
 */
@Component
public class AppleChartsClientImpl extends AbstractExternalApiClient implements AppleChartsPort {

    private static final String SOURCE = "apple_charts";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    /** Apple iTunes "Games" 카테고리 ID. server-side 필터링에 사용. */
    private static final String GAMES_GENRE_ID = "6014";

    public AppleChartsClientImpl(
            @Qualifier(AppleChartsWebClientConfig.APPLE_CHARTS_WEB_CLIENT) WebClient webClient,
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
        String uri = "/" + country + "/rss/topfreeapplications/limit=" + safeLimit
                + "/genre=" + GAMES_GENRE_ID + "/json";
        String cacheKey = "ext:apple:topfree:" + country + ":" + safeLimit;

        return getCached(uri, TopChartsResponse.class, cacheKey, CACHE_TTL).map(resp -> {
            if (resp.feed() == null || resp.feed().entry() == null) {
                return List.<TopAppEntry>of();
            }
            List<TopChartsResponse.Entry> raw = resp.feed().entry();
            List<TopAppEntry> games = new ArrayList<>(raw.size());
            int rank = 0;
            for (TopChartsResponse.Entry e : raw) {
                rank++;
                String appId = e.getAppId();
                String name = e.getName();
                if (appId == null || name == null) {
                    continue;
                }
                games.add(new TopAppEntry(appId, name, e.getArtistName(), rank));
            }
            return games;
        });
    }
}
