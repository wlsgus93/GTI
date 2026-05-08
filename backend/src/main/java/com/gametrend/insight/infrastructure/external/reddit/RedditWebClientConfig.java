package com.gametrend.insight.infrastructure.external.reddit;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Reddit WebClient — OAuth / Anonymous 모드 분기.
 *
 * <p><b>OAuth 모드</b> ({@link RedditProperties#useOauth()} = true):
 * <ul>
 *   <li>baseUrl = {@code oauth.reddit.com}
 *   <li>Authorization: Bearer {token} ({@link RedditOAuthTokenProvider} 자동 갱신)
 *   <li>User-Agent
 * </ul>
 *
 * <p><b>Anonymous 모드</b> (default, useOauth=false):
 * <ul>
 *   <li>baseUrl = {@code www.reddit.com}
 *   <li>User-Agent 만 (OAuth 토큰 X)
 *   <li>Reddit Builder Policy 검토 대기 / 신규 계정 우회용
 * </ul>
 */
@Configuration
public class RedditWebClientConfig {

    public static final String REDDIT_WEB_CLIENT = "redditWebClient";

    /** Reddit /search.json 응답 = ~500KB (comments + 메타). default 256KB 부족 → 16MB 증가. */
    private static final int MAX_IN_MEMORY_BYTES = 16 * 1024 * 1024;

    @Bean(REDDIT_WEB_CLIENT)
    public WebClient redditWebClient(
            WebClient.Builder builder,
            RedditProperties props,
            ObjectProvider<RedditOAuthTokenProvider> tokenProviderProvider) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build();
        if (props.useOauth()) {
            RedditOAuthTokenProvider tokenProvider = tokenProviderProvider.getObject();
            return builder.clone()
                    .baseUrl(props.baseUrl())
                    .exchangeStrategies(strategies)
                    .filter(oauthFilter(props, tokenProvider))
                    .build();
        }
        // Anonymous: User-Agent header only
        return builder.clone()
                .baseUrl(props.anonymousBaseUrl())
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.USER_AGENT, props.userAgent())
                .build();
    }

    private ExchangeFilterFunction oauthFilter(
            RedditProperties props, RedditOAuthTokenProvider tokenProvider) {
        return (request, next) -> {
            String token = tokenProvider.getToken();
            ClientRequest authedRequest = ClientRequest.from(request)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(HttpHeaders.USER_AGENT, props.userAgent())
                    .build();
            return next.exchange(authedRequest);
        };
    }
}
