package com.gametrend.insight.infrastructure.external.reddit;

import com.gametrend.insight.infrastructure.external.common.ExternalApiException;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.oauth.dto.TwitchTokenResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Reddit OAuth Client Credentials 토큰 제공자.
 *
 * <p>Twitch와 다른 점:
 * <ul>
 *   <li>인증: Basic Auth (Authorization: Basic base64(clientId:clientSecret))
 *   <li>User-Agent 필수 헤더 (Reddit이 봇 식별용으로 요구)
 * </ul>
 *
 * <p>같은 점: 3단계 캐시 (인메모리 → Redis → fetch) + ReentrantLock + refresh-ahead.
 *
 * <p>응답 형식은 {@link TwitchTokenResponse}와 동일 (access_token, expires_in) — 재사용.
 */
@Component
@EnableConfigurationProperties(RedditProperties.class)
public class RedditOAuthTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(RedditOAuthTokenProvider.class);
    private static final String CACHE_KEY = "oauth:reddit:token";
    private static final long REFRESH_BUFFER_SECONDS = 60L;
    private static final double REDIS_TTL_RATIO = 0.95;

    private final WebClient webClient;
    private final RedditProperties props;
    private final RedisCacheTemplate cache;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile CachedToken inMemory;

    public RedditOAuthTokenProvider(WebClient.Builder builder, RedditProperties props, RedisCacheTemplate cache) {
        this.webClient = builder.clone().build();
        this.props = props;
        this.cache = cache;
    }

    public String getToken() {
        CachedToken local = this.inMemory;
        if (local != null && !local.isExpiringSoon()) {
            return local.token;
        }

        Optional<CachedToken> redisCached = cache.get(CACHE_KEY, CachedToken.class);
        if (redisCached.isPresent() && !redisCached.get().isExpiringSoon()) {
            this.inMemory = redisCached.get();
            return redisCached.get().token;
        }

        refreshLock.lock();
        try {
            CachedToken latest = this.inMemory;
            if (latest != null && !latest.isExpiringSoon()) {
                return latest.token;
            }
            CachedToken fresh = fetchToken();
            this.inMemory = fresh;
            long ttlSeconds = (long) ((fresh.expiresAt.getEpochSecond() - Instant.now().getEpochSecond())
                    * REDIS_TTL_RATIO);
            if (ttlSeconds > 0) {
                cache.put(CACHE_KEY, fresh, Duration.ofSeconds(ttlSeconds));
            }
            log.info("Reddit OAuth token refreshed, expires at {}", fresh.expiresAt);
            return fresh.token;
        } finally {
            refreshLock.unlock();
        }
    }

    public void invalidate() {
        this.inMemory = null;
        cache.evict(CACHE_KEY);
    }

    private CachedToken fetchToken() {
        String basicAuth = Base64.getEncoder()
                .encodeToString((props.clientId() + ":" + props.clientSecret()).getBytes(StandardCharsets.UTF_8));
        try {
            TwitchTokenResponse resp = webClient
                    .post()
                    .uri(props.oauthUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuth)
                    .header(HttpHeaders.USER_AGENT, props.userAgent())
                    .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
                    .retrieve()
                    .bodyToMono(TwitchTokenResponse.class)
                    .block();

            if (resp == null || resp.accessToken() == null) {
                throw new ExternalApiException.Server("reddit_oauth", "OAuth response empty", null);
            }
            Instant expiresAt = Instant.now().plusSeconds(resp.expiresIn());
            return new CachedToken(resp.accessToken(), expiresAt);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException.Server("reddit_oauth", "OAuth fetch failed: " + e.getMessage(), e);
        }
    }

    public record CachedToken(String token, Instant expiresAt) {
        public boolean isExpiringSoon() {
            return Instant.now().plusSeconds(REFRESH_BUFFER_SECONDS).isAfter(expiresAt);
        }
    }
}
