package com.gametrend.insight.infrastructure.external.steam;

import com.gametrend.insight.application.port.out.SteamWebPort;
import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import com.gametrend.insight.infrastructure.external.common.AbstractExternalApiClient;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.gametrend.insight.infrastructure.external.steam.dto.CurrentPlayersResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Steam Web API 어댑터 — 9개 외부 어댑터의 패턴 정착 케이스.
 *
 * <p>제공:
 * <ul>
 *   <li>{@code GetNumberOfCurrentPlayers} → 동시접속자 수 → PlayerSnapshot
 * </ul>
 *
 * <p>특징:
 * <ul>
 *   <li>{@link AbstractExternalApiClient}의 cache-first GET 패턴 사용
 *   <li>캐시 키: {@code ext:steam:players:{appId}}, TTL 5분
 *   <li>재시도: 5xx/timeout만 ({@link RetryPolicy})
 *   <li>4xx → {@code ExternalApiException.Client}, 429 → {@code ExternalApiException.RateLimit}
 * </ul>
 */
@Component
public class SteamWebClientImpl extends AbstractExternalApiClient implements SteamWebPort {

    private static final String SOURCE = "steam";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    private static final String PLAYERS_PATH = "/ISteamUserStats/GetNumberOfCurrentPlayers/v1/?appid=";

    private final SteamProperties props;

    public SteamWebClientImpl(
            @Qualifier(SteamWebClientConfig.STEAM_WEB_CLIENT) WebClient webClient,
            RedisCacheTemplate cache,
            RetryPolicy retry,
            ExternalApiMetrics metrics,
            SteamProperties props,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        super(webClient, cache, retry, metrics, circuitBreakerRegistry);
        this.props = props;
    }

    @Override
    protected String sourceName() {
        return SOURCE;
    }

    @Override
    public Optional<PlayerSnapshot> fetchCurrentPlayers(long gameId, long appId) {
        String uri = buildPlayersUri(appId);
        String cacheKey = "ext:steam:players:" + appId;

        return getCached(uri, CurrentPlayersResponse.class, cacheKey, CACHE_TTL)
                .flatMap(resp -> SteamMapper.toPlayerSnapshot(resp, gameId));
    }

    private String buildPlayersUri(long appId) {
        StringBuilder sb = new StringBuilder(PLAYERS_PATH).append(appId);
        if (props != null && StringUtils.hasText(props.apiKey())) {
            sb.append("&key=").append(props.apiKey());
        }
        return sb.toString();
    }
}
