package com.gametrend.insight.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * 공유 WebClient.Builder 빈.
 *
 * <p>각 어댑터는 이 Builder를 주입받아 base URL, 인증 헤더 등을 추가해 자신만의 WebClient를 만든다.
 */
@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder(
            @Value("${gti.external.timeout-ms:5000}") int timeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
                .responseTimeout(Duration.ofMillis(timeoutMs));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
