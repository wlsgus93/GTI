package com.gametrend.insight.infrastructure.external.youtube;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(YouTubeProperties.class)
public class YouTubeWebClientConfig {

    public static final String YOUTUBE_WEB_CLIENT = "youtubeWebClient";

    @Bean(YOUTUBE_WEB_CLIENT)
    public WebClient youtubeWebClient(WebClient.Builder builder, YouTubeProperties props) {
        return builder.clone().baseUrl(props.baseUrl()).build();
    }
}
