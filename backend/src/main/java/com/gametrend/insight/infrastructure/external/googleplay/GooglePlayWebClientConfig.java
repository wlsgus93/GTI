package com.gametrend.insight.infrastructure.external.googleplay;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Google Play crawler-service WebClient.
 *
 * <p>{@link GooglePlayChartsProperties#baseUrl()} 로 설정된 crawler-service base URL 사용.
 */
@Configuration
@EnableConfigurationProperties(GooglePlayChartsProperties.class)
public class GooglePlayWebClientConfig {

    public static final String GOOGLE_PLAY_WEB_CLIENT = "googlePlayWebClient";

    @Bean(GOOGLE_PLAY_WEB_CLIENT)
    public WebClient googlePlayWebClient(
            WebClient.Builder builder, GooglePlayChartsProperties props) {
        HttpClient httpClient =
                HttpClient.create()
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.timeoutMs())
                        .responseTimeout(Duration.ofMillis(props.timeoutMs()))
                        .doOnConnected(
                                conn ->
                                        conn.addHandlerLast(
                                                        new ReadTimeoutHandler(
                                                                props.timeoutMs(),
                                                                TimeUnit.MILLISECONDS))
                                                .addHandlerLast(
                                                        new WriteTimeoutHandler(
                                                                props.timeoutMs(),
                                                                TimeUnit.MILLISECONDS)));
        return builder.clone()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
