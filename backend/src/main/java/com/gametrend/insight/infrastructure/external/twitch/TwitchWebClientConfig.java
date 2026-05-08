package com.gametrend.insight.infrastructure.external.twitch;

import com.gametrend.insight.infrastructure.external.oauth.TwitchOAuthProperties;
import com.gametrend.insight.infrastructure.external.oauth.TwitchOAuthTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Twitch Helix 전용 WebClient.
 *
 * <p>매 요청마다 Bearer 토큰 + Client-Id 헤더를 자동 주입 (ExchangeFilterFunction).
 * 토큰 만료 시 {@link TwitchOAuthTokenProvider}가 자동 갱신.
 */
@Configuration
public class TwitchWebClientConfig {

    public static final String TWITCH_WEB_CLIENT = "twitchWebClient";

    @Bean(TWITCH_WEB_CLIENT)
    public WebClient twitchWebClient(
            WebClient.Builder builder,
            TwitchOAuthProperties props,
            TwitchOAuthTokenProvider tokenProvider) {
        return builder.clone()
                .baseUrl(props.baseUrl())
                .filter(authFilter(props, tokenProvider))
                .build();
    }

    private ExchangeFilterFunction authFilter(
            TwitchOAuthProperties props, TwitchOAuthTokenProvider tokenProvider) {
        return (request, next) -> {
            String token = tokenProvider.getToken();
            ClientRequest authedRequest = ClientRequest.from(request)
                    .header("Authorization", "Bearer " + token)
                    .header("Client-Id", props.clientId())
                    .build();
            return next.exchange(authedRequest);
        };
    }
}
