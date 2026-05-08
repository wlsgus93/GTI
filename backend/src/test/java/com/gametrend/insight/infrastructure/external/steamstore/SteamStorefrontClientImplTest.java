package com.gametrend.insight.infrastructure.external.steamstore;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * SteamStorefrontClientImpl 테스트 — Day 3 패턴 재사용 검증.
 *
 * <p>Day 3의 SteamWebClientImplTest와 같은 구조. **재사용 가능한 패턴 입증**이 핵심.
 */
class SteamStorefrontClientImplTest {

    private static final long GAME_ID = 1L;
    private static final long APP_ID = 730L;

    private static WireMockServer wireMock;
    private SteamStorefrontClientImpl client;

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
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMock.port())
                .build();
        FakeRedisCache cache = new FakeRedisCache();
        RetryPolicy retry = new RetryPolicy(3L, 50L, 2000L);
        ExternalApiMetrics metrics = new ExternalApiMetrics(new SimpleMeterRegistry());
        client = new SteamStorefrontClientImpl(
                webClient, cache, retry, metrics, CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("유료 게임 200 OK → PriceSnapshot (currency, priceCents, discount)")
    void paidGame_200() {
        wireMock.stubFor(get(urlPathMatching("/appdetails.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                {"730":{"success":true,"data":{"name":"CS2","is_free":false,
                                "price_overview":{"currency":"USD","initial":5999,"final":4499,
                                "discount_percent":25}}}}
                                """)));

        Optional<PriceSnapshot> result = client.fetchPrice(GAME_ID, APP_ID);

        assertThat(result).isPresent();
        assertThat(result.get().currency()).isEqualTo("USD");
        assertThat(result.get().priceCents()).isEqualTo(4499L);
        assertThat(result.get().discountPercent()).isEqualTo(25);
        assertThat(result.get().source()).isEqualTo(SnapshotSource.STEAM_STORE);
    }

    @Test
    @DisplayName("무료 게임 (price_overview 없음, is_free=true) → PriceSnapshot (priceCents=0)")
    void freeGame_200() {
        wireMock.stubFor(get(urlPathMatching("/appdetails.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"730":{"success":true,"data":{"name":"Free Game","is_free":true}}}
                                """)));

        Optional<PriceSnapshot> result = client.fetchPrice(GAME_ID, APP_ID);

        assertThat(result).isPresent();
        assertThat(result.get().priceCents()).isEqualTo(0L);
        assertThat(result.get().discountPercent()).isEqualTo(0);
    }

    @Test
    @DisplayName("success=false → empty (게임 미존재)")
    void notFound_successFalse() {
        wireMock.stubFor(get(urlPathMatching("/appdetails.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"730\":{\"success\":false}}")));

        Optional<PriceSnapshot> result = client.fetchPrice(GAME_ID, APP_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("패턴 재사용 검증 — 두 번째 호출 캐시 hit (HTTP 1번만)")
    void cacheReusesAcrossCalls() {
        wireMock.stubFor(get(urlPathMatching("/appdetails.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                {"730":{"success":true,"data":{"is_free":false,
                                "price_overview":{"currency":"USD","initial":1999,"final":1999,"discount_percent":0}}}}
                                """)));

        client.fetchPrice(GAME_ID, APP_ID);
        client.fetchPrice(GAME_ID, APP_ID);

        wireMock.verify(1, getRequestedFor(urlPathMatching("/appdetails.*")));
    }

    /** Day 3과 동일한 in-memory 캐시 — 헬퍼 추출은 다음 어댑터 추가 시 검토. */
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
