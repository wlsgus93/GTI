package com.gametrend.insight.infrastructure.external.igdb;

import com.gametrend.insight.infrastructure.external.oauth.TwitchOAuthProperties;
import com.gametrend.insight.infrastructure.external.oauth.TwitchOAuthTokenProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * IGDB 전용 WebClient. Twitch OAuth 토큰을 공유 (같은 client_id/secret).
 * 매 요청 시 Bearer + Client-Id 헤더 자동 주입.
 */
@Configuration
@EnableConfigurationProperties(IgdbProperties.class)
public class IgdbWebClientConfig {

    public static final String IGDB_WEB_CLIENT = "igdbWebClient";

    @Bean(IGDB_WEB_CLIENT)
    public WebClient igdbWebClient(
            WebClient.Builder builder,
            IgdbProperties igdbProps,
            TwitchOAuthProperties twitchProps,
            TwitchOAuthTokenProvider tokenProvider) {
        return builder.clone()
                .baseUrl(igdbProps.baseUrl())
                .filter(authFilter(twitchProps, tokenProvider))
                .build();
    }

    private ExchangeFilterFunction authFilter(
            TwitchOAuthProperties twitchProps, TwitchOAuthTokenProvider tokenProvider) {
        return (request, next) -> {
            String token = tokenProvider.getToken();
            ClientRequest authedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + token)
                    .header("Client-ID", twitchProps.clientId())
                    .build();
            return next.exchange(authedRequest);
        };
    }
}
