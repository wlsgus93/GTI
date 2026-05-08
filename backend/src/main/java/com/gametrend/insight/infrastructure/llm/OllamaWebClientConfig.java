package com.gametrend.insight.infrastructure.llm;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Ollama WebClient — provider=ollama 일 때만 활성화.
 */
@Configuration
@EnableConfigurationProperties(OllamaProperties.class)
@ConditionalOnProperty(name = "gti.llm.provider", havingValue = "ollama")
public class OllamaWebClientConfig {

    public static final String OLLAMA_WEB_CLIENT = "ollamaWebClient";

    /** LLM 응답 가능 크기 — 4096 토큰 × 약 4 char = ~16KB. 안전선 16MB. */
    private static final int MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;

    @Bean(OLLAMA_WEB_CLIENT)
    public WebClient ollamaWebClient(WebClient.Builder builder, OllamaProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.timeoutMs())
                .responseTimeout(Duration.ofMillis(props.timeoutMs()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(props.timeoutMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(props.timeoutMs(), TimeUnit.MILLISECONDS)));

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build();

        return builder.clone()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .build();
    }
}
