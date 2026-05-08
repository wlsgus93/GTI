package com.gametrend.insight.infrastructure.external.youtube;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
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

class YouTubeClientImplTest {

    private static WireMockServer wireMock;
    private YouTubeClientImpl client;

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
        YouTubeProperties props = new YouTubeProperties("http://localhost:" + wireMock.port(), "test-key");
        client = new YouTubeClientImpl(
                webClient,
                new FakeRedisCache(),
                new RetryPolicy(3L, 50L, 2000L),
                new ExternalApiMetrics(new SimpleMeterRegistry()),
                props,
                CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("GET /search → totalResults를 MentionSnapshot으로")
    void searchReturnsMention() {
        wireMock.stubFor(get(urlPathMatching("/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageInfo\":{\"totalResults\":98765,\"resultsPerPage\":1}}")));

        Optional<MentionSnapshot> result = client.fetchMentionCount(1L, "ELDEN RING");

        assertThat(result).isPresent();
        assertThat(result.get().mentionCount()).isEqualTo(98765);
        assertThat(result.get().source()).isEqualTo(SnapshotSource.YOUTUBE);
        assertThat(result.get().gameId()).isEqualTo(1L);
    }
}
