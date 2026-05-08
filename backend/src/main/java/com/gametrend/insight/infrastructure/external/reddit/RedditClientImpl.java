package com.gametrend.insight.infrastructure.external.reddit;

import com.gametrend.insight.application.port.out.RedditPort;
import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.reddit.dto.RedditSearchResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Reddit search 어댑터.
 *
 * <p>OAuth / Anonymous 모드 자동 분기 ({@link RedditProperties#useOauth()}):
 * <ul>
 *   <li>OAuth: {@code GET oauth.reddit.com/search?q=...} (Builder Policy 통과 + script app 등록 후)
 *   <li>Anonymous: {@code GET www.reddit.com/search.json?q=...} (default — 즉시 가능)
 * </ul>
 *
 * <p>응답 schema 는 두 모드 모두 동일 ({@code data.children[]}). path suffix({@code .json}) 만 다름.
 */
@Component
@EnableConfigurationProperties(RedditProperties.class)
public class RedditClientImpl extends AbstractExternalApiClient implements RedditPort {

    private static final String SOURCE = "reddit";
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final int LIMIT = 100;

    private final RedditProperties props;

    public RedditClientImpl(
            @Qualifier(RedditWebClientConfig.REDDIT_WEB_CLIENT) WebClient webClient,
            RedisCacheTemplate cache,
            RetryPolicy retry,
            ExternalApiMetrics metrics,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RedditProperties props) {
        super(webClient, cache, retry, metrics, circuitBreakerRegistry);
        this.props = props;
    }

    @Override
    protected String sourceName() {
        return SOURCE;
    }

    @Override
    public Optional<MentionSnapshot> fetchMentionCount(long gameId, String gameName) {
        String encoded = URLEncoder.encode(gameName, StandardCharsets.UTF_8);
        // OAuth /search vs Anonymous /search.json — 응답 schema 동일, path suffix 만 다름
        String pathSuffix = props.useOauth() ? "" : ".json";
        String uri = "/search" + pathSuffix + "?q=" + encoded + "&limit=" + LIMIT + "&sort=hot";
        String cacheKey = "ext:reddit:mention:" + gameName;

        return getCached(uri, RedditSearchResponse.class, cacheKey, CACHE_TTL).map(resp -> {
            int count = (resp.data() == null || resp.data().children() == null)
                    ? 0
                    : resp.data().children().size();
            return new MentionSnapshot(
                    null, gameId, count, null, Instant.now(), SnapshotSource.REDDIT, false);
        });
    }
}
