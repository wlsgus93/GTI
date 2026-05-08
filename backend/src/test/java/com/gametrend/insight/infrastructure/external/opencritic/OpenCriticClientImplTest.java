package com.gametrend.insight.infrastructure.external.opencritic;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.application.port.out.OpenCriticPort.OpenCriticScore;
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

class OpenCriticClientImplTest {

    private static WireMockServer wireMock;
    private OpenCriticClientImpl client;

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
        client = new OpenCriticClientImpl(
                webClient,
                new FakeRedisCache(),
                new RetryPolicy(3L, 50L, 2000L),
                new ExternalApiMetrics(new SimpleMeterRegistry()),
                CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("GET /game/{id} → OpenCriticScore 매핑")
    void scoreReturnsCriticData() {
        wireMock.stubFor(get(urlPathMatching("/game/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                {"id":1234,"name":"ELDEN RING","topCriticScore":94.5,
                                "tier":"Mighty","numReviews":75}
                                """)));

        Optional<OpenCriticScore> result = client.fetchScore(1L, 1234L);

        assertThat(result).isPresent();
        assertThat(result.get().topCriticScore()).isEqualTo(94.5);
        assertThat(result.get().tier()).isEqualTo("Mighty");
        assertThat(result.get().reviewCount()).isEqualTo(75);
    }
}
