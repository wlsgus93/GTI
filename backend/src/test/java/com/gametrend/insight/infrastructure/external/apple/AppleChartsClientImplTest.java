package com.gametrend.insight.infrastructure.external.apple;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.application.port.out.AppleChartsPort.TopAppEntry;
import com.gametrend.insight.infrastructure.external.common.ExternalApiMetrics;
import com.gametrend.insight.infrastructure.external.common.FakeRedisCache;
import com.gametrend.insight.infrastructure.external.common.RetryPolicy;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class AppleChartsClientImplTest {

    private static WireMockServer wireMock;
    private AppleChartsClientImpl client;

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
        client = new AppleChartsClientImpl(
                webClient,
                new FakeRedisCache(),
                new RetryPolicy(3L, 50L, 2000L),
                new ExternalApiMetrics(new SimpleMeterRegistry()),
                CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    @DisplayName("legacy iTunes RSS — feed.entry[] 매핑 + im:id / im:name / im:artist 추출")
    void mapsLegacyEntries() {
        wireMock.stubFor(get(urlPathMatching("/us/rss/topfreeapplications/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                {"feed":{"entry":[
                                  {
                                    "im:name":{"label":"Magic Sort!"},
                                    "im:artist":{"label":"Voodoo SAS"},
                                    "id":{"label":"https://apps.apple.com/...","attributes":{"im:id":"1234567890"}}
                                  },
                                  {
                                    "im:name":{"label":"Royal Match"},
                                    "im:artist":{"label":"Dream Games"},
                                    "id":{"label":"https://apps.apple.com/...","attributes":{"im:id":"1525437619"}}
                                  }
                                ]}}
                                """)));

        Optional<List<TopAppEntry>> result = client.fetchTopFreeGames("us", 10);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(2);
        assertThat(result.get().get(0).id()).isEqualTo("1234567890");
        assertThat(result.get().get(0).name()).isEqualTo("Magic Sort!");
        assertThat(result.get().get(0).artistName()).isEqualTo("Voodoo SAS");
        assertThat(result.get().get(0).rank()).isEqualTo(1);
        assertThat(result.get().get(1).id()).isEqualTo("1525437619");
        assertThat(result.get().get(1).rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("entry 누락 / null → 빈 리스트")
    void emptyResultWhenNoEntries() {
        wireMock.stubFor(get(urlPathMatching("/us/rss/topfreeapplications/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"feed":{}}
                                """)));

        Optional<List<TopAppEntry>> result = client.fetchTopFreeGames("us", 10);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
    }

    @Test
    @DisplayName("appId 또는 name 누락된 entry 는 skip")
    void skipsInvalidEntries() {
        wireMock.stubFor(get(urlPathMatching("/us/rss/topfreeapplications/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                """
                                {"feed":{"entry":[
                                  {
                                    "im:name":{"label":"Valid Game"},
                                    "im:artist":{"label":"Studio"},
                                    "id":{"label":"...","attributes":{"im:id":"100"}}
                                  },
                                  {
                                    "im:artist":{"label":"No Name"},
                                    "id":{"label":"...","attributes":{"im:id":"200"}}
                                  }
                                ]}}
                                """)));

        Optional<List<TopAppEntry>> result = client.fetchTopFreeGames("us", 10);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get().get(0).id()).isEqualTo("100");
    }

    @Test
    @DisplayName("URL 에 limit + genre=6014 포함")
    void buildsCorrectUri() {
        wireMock.stubFor(get(urlPathMatching("/us/rss/topfreeapplications/limit=25/genre=6014/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"feed":{"entry":[]}}
                                """)));

        Optional<List<TopAppEntry>> result = client.fetchTopFreeGames("us", 25);

        assertThat(result).isPresent();
        assertThat(result.get()).isEmpty();
        wireMock.verify(com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(
                com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo(
                        "/us/rss/topfreeapplications/limit=25/genre=6014/json")));
    }
}
