package com.gametrend.insight.infrastructure.external.twitch;

import com.gametrend.insight.application.port.out.TwitchPort;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.domain.snapshot.ViewerSnapshot;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.twitch.dto.StreamsResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Twitch Helix 어댑터 — OAuth 자동 주입 케이스.
 *
 * <p>인증은 {@link TwitchWebClientConfig}의 ExchangeFilterFunction에서 처리.
 * 이 클라이언트는 단순히 URI/매핑만 담당 (Day 3, Day 4 Steam Storefront와 동일 패턴).
 */
@Component
public class TwitchHelixClientImpl extends AbstractExternalApiClient implements TwitchPort {

    private static final String SOURCE = "twitch";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final int MAX_FIRST_PAGE = 100;

    public TwitchHelixClientImpl(
            @Qualifier(TwitchWebClientConfig.TWITCH_WEB_CLIENT) WebClient webClient,
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
    public Optional<ViewerSnapshot> fetchViewers(long gameId, String twitchGameId) {
        String uri = "/streams?game_id=" + twitchGameId + "&first=" + MAX_FIRST_PAGE;
        String cacheKey = "ext:twitch:viewers:" + twitchGameId;

        return getCached(uri, StreamsResponse.class, cacheKey, CACHE_TTL).map(resp -> {
            int total = resp.data() == null
                    ? 0
                    : resp.data().stream().mapToInt(StreamsResponse.Stream::viewerCount).sum();
            return new ViewerSnapshot(null, gameId, total, Instant.now(), SnapshotSource.TWITCH, false);
        });
    }
}
