package com.gametrend.insight.infrastructure.external.steamspy;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.application.port.out.SteamSpyPort.SteamSpyEstimates;
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

class SteamSpyClientImplTest {

    private static WireMockServer wireMock;
    private SteamSpyClientImpl client;

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
        client = new SteamSpyClientImpl(
                webClient,
                new FakeRedisCache(),
                new RetryPolicy(3L, 50L, 2000L),
                new ExternalApiMetrics(new SimpleMeterRegistry()),
                CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("GET ?request=appdetails → SteamSpyEstimates 매핑 (owners 범위 + ccu)")
    void appDetailsReturnsEstimates() {
        wireMock.stubFor(get(urlPathMatching("/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                {"appid":730,"name":"Counter-Strike 2",
                                "owners":"50,000,000 .. 100,000,000",
                                "average_forever":5000,"median_2weeks":120,"ccu":1500000}
                                """)));

        Optional<SteamSpyEstimates> result = client.fetchEstimates(1L, 730L);

        assertThat(result).isPresent();
        assertThat(result.get().appId()).isEqualTo(730L);
        assertThat(result.get().ownersRange()).isEqualTo("50,000,000 .. 100,000,000");
        assertThat(result.get().ccu()).isEqualTo(1500000);
    }
}
