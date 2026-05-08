package com.gametrend.insight.infrastructure.external.igdb;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.application.port.out.IgdbPort;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.FakeRedisCache;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class IgdbClientImplTest {

    private static final long IGDB_ID = 1942L;
    private static WireMockServer wireMock;
    private IgdbClientImpl client;

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
        client = new IgdbClientImpl(
                webClient,
                new FakeRedisCache(),
                new RetryPolicy(3L, 50L, 2000L),
                new ExternalApiMetrics(new SimpleMeterRegistry()),
                CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("POST /games + Apicalypse body → IgdbGameMeta 매핑")
    void postWithApicalypseBody_returnsMeta() {
        wireMock.stubFor(post(urlPathMatching("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                [{"id":1942,"name":"ELDEN RING","summary":"Open world",
                                "first_release_date":1645747200,
                                "genres":[{"id":12,"name":"Role-playing (RPG)"}],
                                "cover":{"id":99,"url":"//images.igdb.com/cover.jpg"}}]
                                """)));

        Optional<IgdbPort.IgdbGameMeta> result = client.fetchGameMetadata(IGDB_ID);

        assertThat(result).isPresent();
        assertThat(result.get().igdbId()).isEqualTo(1942L);
        assertThat(result.get().name()).isEqualTo("ELDEN RING");
        assertThat(result.get().genres()).containsExactly("Role-playing (RPG)");
        assertThat(result.get().coverUrl()).isEqualTo("https://images.igdb.com/cover.jpg");

        // Apicalypse 본문 검증
        wireMock.verify(postRequestedFor(urlPathMatching("/games"))
                .withRequestBody(equalTo(
                        "fields name,summary,first_release_date,genres.name,cover.url; where id = 1942;")));
    }

    @Test
    @DisplayName("빈 응답 (게임 없음) → empty")
    void emptyArrayResponse_empty() {
        wireMock.stubFor(post(urlPathMatching("/games"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        Optional<IgdbPort.IgdbGameMeta> result = client.fetchGameMetadata(IGDB_ID);

        assertThat(result).isEmpty();
    }
}
