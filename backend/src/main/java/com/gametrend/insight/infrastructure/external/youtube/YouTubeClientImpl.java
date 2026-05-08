package com.gametrend.insight.infrastructure.external.youtube;

import com.gametrend.insight.application.port.out.YouTubePort;
import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.youtube.dto.YouTubeSearchResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class YouTubeClientImpl extends AbstractExternalApiClient implements YouTubePort {

    private static final String SOURCE = "youtube";
    private static final Duration CACHE_TTL = Duration.ofHours(6); // 검색량 6시간 캐시

    private final YouTubeProperties props;

    public YouTubeClientImpl(
            @Qualifier(YouTubeWebClientConfig.YOUTUBE_WEB_CLIENT) WebClient webClient,
            RedisCacheTemplate cache,
            RetryPolicy retry,
            ExternalApiMetrics metrics,
            YouTubeProperties props,
            CircuitBreakerRegistry circuitBreakerRegistry) {
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
        String uri = "/search?part=snippet&type=video&maxResults=1&q=" + encoded + "&key=" + props.apiKey();
        String cacheKey = "ext:youtube:mention:" + gameName;

        return getCached(uri, YouTubeSearchResponse.class, cacheKey, CACHE_TTL).map(resp -> {
            int total = resp.pageInfo() == null ? 0 : resp.pageInfo().totalResults();
            return new MentionSnapshot(
                    null, gameId, total, null, Instant.now(), SnapshotSource.YOUTUBE, false);
        });
    }
}
