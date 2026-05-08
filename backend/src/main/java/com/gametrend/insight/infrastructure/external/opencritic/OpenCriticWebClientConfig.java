package com.gametrend.insight.infrastructure.external.opencritic;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(OpenCriticProperties.class)
public class OpenCriticWebClientConfig {

    public static final String OPENCRITIC_WEB_CLIENT = "openCriticWebClient";

    @Bean(OPENCRITIC_WEB_CLIENT)
    public WebClient openCriticWebClient(WebClient.Builder builder, OpenCriticProperties props) {
        WebClient.Builder b = builder.clone().baseUrl(props.baseUrl());
        // API key는 RapidAPI 경유 시 헤더로 전달 (없으면 무인증 호출)
        if (StringUtils.hasText(props.apiKey())) {
            b.defaultHeader("X-RapidAPI-Key", props.apiKey());
        }
        return b.build();
    }
}
