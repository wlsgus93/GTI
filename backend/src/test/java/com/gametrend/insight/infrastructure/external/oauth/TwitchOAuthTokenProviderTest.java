package com.gametrend.insight.infrastructure.external.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * TwitchOAuthTokenProvider 테스트 — 캐시 + 동시 갱신 안전성 검증.
 */
class TwitchOAuthTokenProviderTest {

    private static WireMockServer wireMock;
    private TwitchOAuthTokenProvider provider;

    @BeforeAll
    static void start() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();
    }

    @AfterAll
    static void stop() {
        wireMock.stop();
    }

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        WebClient.Builder builder = WebClient.builder();
        FakeRedisCache cache = new FakeRedisCache();
        TwitchOAuthProperties props = new TwitchOAuthProperties(
                "http://localhost:" + wireMock.port() + "/helix",
                "http://localhost:" + wireMock.port() + "/oauth/token",
                "test-client-id",
                "test-client-secret");
        provider = new TwitchOAuthTokenProvider(builder, props, cache);
    }

    @Test
    @DisplayName("첫 호출 → OAuth fetch + 토큰 반환")
    void firstCall_fetchesToken() {
        stubOk("token-1", 3600);

        String token = provider.getToken();

        assertThat(token).isEqualTo("token-1");
        wireMock.verify(1, postRequestedFor(urlPathMatching("/oauth/token")));
    }

    @Test
    @DisplayName("연속 호출 → 인메모리 캐시 hit (HTTP 1번만)")
    void cachedAcrossCalls() {
        stubOk("token-cached", 3600);

        provider.getToken();
        provider.getToken();
        provider.getToken();

        wireMock.verify(1, postRequestedFor(urlPathMatching("/oauth/token")));
    }

    @Test
    @DisplayName("동시 호출 (10 스레드) → 락으로 단일 갱신 보장 (HTTP 1번)")
    void concurrentCalls_singleFetch() throws ExecutionException, InterruptedException {
        stubOk("token-concurrent", 3600);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = IntStream.range(0, 10)
                    .mapToObj(i -> CompletableFuture.supplyAsync(() -> provider.getToken(), executor))
                    .collect(Collectors.toList());

            for (var f : futures) {
                assertThat(f.get()).isEqualTo("token-concurrent");
            }
        }

        // 단일 인스턴스 락 → 정확히 1번 fetch
        wireMock.verify(1, postRequestedFor(urlPathMatching("/oauth/token")));
    }

    @Test
    @DisplayName("invalidate 후 다시 호출 → 새로 fetch")
    void invalidateForcesRefetch() {
        stubOk("token-1st", 3600);

        provider.getToken();
        provider.invalidate();
        // 새 토큰
        wireMock.resetAll();
        stubOk("token-2nd", 3600);

        String token = provider.getToken();

        assertThat(token).isEqualTo("token-2nd");
    }

    private static void stubOk(String token, int expiresIn) {
        wireMock.stubFor(post(urlPathMatching("/oauth/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"access_token\":\"" + token + "\",\"expires_in\":" + expiresIn
                                        + ",\"token_type\":\"bearer\"}")));
    }

    static class FakeRedisCache extends RedisCacheTemplate {
        private final Map<String, Object> store = new HashMap<>();

        FakeRedisCache() {
            super(null, null);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> get(String key, Class<T> type) {
            Object v = store.get(key);
            return v == null ? Optional.empty() : Optional.of((T) v);
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
