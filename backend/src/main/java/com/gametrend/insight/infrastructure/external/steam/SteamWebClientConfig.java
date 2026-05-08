package com.gametrend.insight.infrastructure.external.steam;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Steam 전용 WebClient 빈.
 *
 * <p>{@link com.gametrend.insight.config.WebClientConfig}의 공유 Builder를 받아
 * Steam baseUrl을 적용한 인스턴스를 생성한다. API key는 호출 시 query param으로 추가
 * (Steam은 헤더 인증이 아니라 {@code ?key=...} 방식).
 */
@Configuration
@EnableConfigurationProperties(SteamProperties.class)
public class SteamWebClientConfig {

    public static final String STEAM_WEB_CLIENT = "steamWebClient";
    public static final String STEAM_STOREFRONT_WEB_CLIENT = "steamStorefrontWebClient";

    @Bean(STEAM_WEB_CLIENT)
    public WebClient steamWebClient(WebClient.Builder builder, SteamProperties props) {
        return builder.clone().baseUrl(props.baseUrl()).build();
    }

    /**
     * Steam Storefront 전용 WebClient. {@code https://store.steampowered.com/api}.
     * 인증 없음. SteamStorefrontClientImpl이 사용.
     */
    @Bean(STEAM_STOREFRONT_WEB_CLIENT)
    public WebClient steamStorefrontWebClient(WebClient.Builder builder, SteamProperties props) {
        return builder.clone().baseUrl(props.storefrontUrl()).build();
    }
}
