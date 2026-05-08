package com.gametrend.insight.infrastructure.external.steam;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.ExternalApiException;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.netty.channel.ChannelOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * SteamWebClientImpl 5 시나리오 테스트 — Day 3 패턴 정착의 핵심.
 *
 * <p>WireMock으로 Steam API를 시뮬레이션. Spring 컨텍스트 없이 순수 단위 테스트.
 *
 * <p>시나리오:
 * <ol>
 *   <li>200 OK → PlayerSnapshot 정상 매핑
 *   <li>429 + Retry-After → ExternalApiException.RateLimit 즉시 실패
 *   <li>500 재시도 소진 → ExternalApiException.Server (4 requests = 1 + 3 retries)
 *   <li>응답 지연 → timeout → ExternalApiException.Server
 *   <li>캐시 hit → 두 번째 호출은 HTTP X
 * </ol>
 */
class SteamWebClientImplTest {

    private static final long GAME_ID = 1L;
    private static final long APP_ID = 730L;
    private static final String STEAM_PATH_PATTERN = "/ISteamUserStats/.*";

    private static WireMockServer wireMock;
    private SteamWebClientImpl client;
    private FakeRedisCache cache;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();

        // WebClient: WireMock 포트로 base URL, 짧은 read timeout (테스트 빠르게)
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .responseTimeout(Duration.ofMillis(2000));
        WebClient webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("http://localhost:" + wireMock.port())
                .build();

        cache = new FakeRedisCache();
        // 짧은 backoff (테스트 속도)
        RetryPolicy retry = new RetryPolicy(3L, 50L, 2000L);
        ExternalApiMetrics metrics = new ExternalApiMetrics(new SimpleMeterRegistry());
        SteamProperties props = new SteamProperties(
                "http://localhost:" + wireMock.port(),
                null,
                null, // apiKey null → 키 없는 호출
                2000);

        client = new SteamWebClientImpl(
                webClient, cache, retry, metrics, props, CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("200 OK → PlayerSnapshot 정상 반환 (concurrentPlayers + STEAM source)")
    void success_200() {
        wireMock.stubFor(get(urlPathMatching(STEAM_PATH_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":{\"player_count\":12345,\"result\":1}}")));

        Optional<PlayerSnapshot> result = client.fetchCurrentPlayers(GAME_ID, APP_ID);

        assertThat(result).isPresent();
        assertThat(result.get().concurrentPlayers()).isEqualTo(12345);
        assertThat(result.get().source()).isEqualTo(SnapshotSource.STEAM);
        assertThat(result.get().gameId()).isEqualTo(GAME_ID);
        assertThat(result.get().stale()).isFalse();
        wireMock.verify(1, getRequestedFor(urlPathMatching(STEAM_PATH_PATTERN)));
    }

    @Test
    @DisplayName("429 + Retry-After:2 → RateLimit 즉시 실패 (재시도 X)")
    void rateLimit_429() {
        wireMock.stubFor(get(urlPathMatching(STEAM_PATH_PATTERN))
                .willReturn(aResponse().withStatus(429).withHeader("Retry-After", "2").withBody("{}")));

        ExternalApiException.RateLimit thrown = assertThrows(
                ExternalApiException.RateLimit.class, () -> client.fetchCurrentPlayers(GAME_ID, APP_ID));

        assertThat(thrown.source()).isEqualTo("steam");
        assertThat(thrown.retryAfterMs()).isEqualTo(2000L);
        // 429는 재시도 X → 1 request만
        wireMock.verify(1, getRequestedFor(urlPathMatching(STEAM_PATH_PATTERN)));
    }

    @Test
    @DisplayName("500 재시도 소진 → Server 예외 (1 + 3 retries = 4 requests)")
    void serverError_500_retryExhausted() {
        wireMock.stubFor(get(urlPathMatching(STEAM_PATH_PATTERN))
                .willReturn(aResponse().withStatus(500).withBody("Internal Error")));

        assertThrows(ExternalApiException.Server.class, () -> client.fetchCurrentPlayers(GAME_ID, APP_ID));

        // Reactor Retry.backoff(3, ...) = 1 initial + 3 retries
        wireMock.verify(4, getRequestedFor(urlPathMatching(STEAM_PATH_PATTERN)));
    }

    @Test
    @DisplayName("Read timeout (응답 3s, 타임아웃 2s) → Server 예외")
    void timeout() {
        wireMock.stubFor(get(urlPathMatching(STEAM_PATH_PATTERN))
                .willReturn(aResponse()
                        .withFixedDelay(3000) // > 2s timeout
                        .withStatus(200)
                        .withBody("{\"response\":{\"player_count\":1,\"result\":1}}")));

        assertThrows(ExternalApiException.Server.class, () -> client.fetchCurrentPlayers(GAME_ID, APP_ID));
    }

    @Test
    @DisplayName("캐시 hit → 두 번째 호출은 HTTP X (1 request만)")
    void cacheHit() {
        wireMock.stubFor(get(urlPathMatching(STEAM_PATH_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"response\":{\"player_count\":12345,\"result\":1}}")));

        Optional<PlayerSnapshot> first = client.fetchCurrentPlayers(GAME_ID, APP_ID);
        Optional<PlayerSnapshot> second = client.fetchCurrentPlayers(GAME_ID, APP_ID);

        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(first.get().concurrentPlayers()).isEqualTo(second.get().concurrentPlayers());
        // 두 번째 호출은 캐시 hit → HTTP 1번만
        wireMock.verify(1, getRequestedFor(urlPathMatching(STEAM_PATH_PATTERN)));
    }

    /**
     * 테스트용 in-memory 캐시. RedisCacheTemplate의 메서드를 오버라이드 (실제 Redis 안 씀).
     */
    static class FakeRedisCache extends RedisCacheTemplate {
        private final Map<String, Object> store = new HashMap<>();

        FakeRedisCache() {
            super(null, null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            Object v = store.get(key);
            if (v == null) {
                return Optional.empty();
            }
            return Optional.of((T) v);
        }

        @Override
        public <T> void put(String key, T value, Duration ttl) {
            store.put(key, value);
        }

        @Override
        public void evict(String key) {
            store.remove(key);
        }
    }
}
