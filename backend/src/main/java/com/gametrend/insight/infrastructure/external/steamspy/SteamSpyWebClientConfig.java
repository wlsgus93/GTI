package com.gametrend.insight.infrastructure.external.steamspy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(SteamSpyProperties.class)
public class SteamSpyWebClientConfig {

    public static final String STEAMSPY_WEB_CLIENT = "steamSpyWebClient";

    @Bean(STEAMSPY_WEB_CLIENT)
    public WebClient steamSpyWebClient(WebClient.Builder builder, SteamSpyProperties props) {
        return builder.clone().baseUrl(props.baseUrl()).build();
    }
}
