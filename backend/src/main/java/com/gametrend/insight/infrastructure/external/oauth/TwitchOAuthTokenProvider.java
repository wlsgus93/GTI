package com.gametrend.insight.infrastructure.external.oauth;

import com.gametrend.insight.infrastructure.external.common.ExternalApiException;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.external.oauth.dto.TwitchTokenResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Twitch OAuth (Client Credentials) 토큰 제공자. Twitch Helix와 IGDB가 공유.
 *
 * <p>3단계 캐시:
 * <ol>
 *   <li>인메모리 (volatile, fast path)
 *   <li>Redis (인스턴스 간 공유, 매 호출 검사)
 *   <li>HTTP fetch (둘 다 만료 시)
 * </ol>
 *
 * <p>동시성: {@link ReentrantLock} 사용. {@code synchronized} 블록 + WebClient.block()은
 * Virtual Thread pinning을 유발하므로 회피. 단일 인스턴스 환경 가정 (멀티 인스턴스 시
 * Redisson {@code RLock}으로 교체).
 *
 * <p>만료 60초 전 갱신 (refresh-ahead).
 */
@Component
@EnableConfigurationProperties(TwitchOAuthProperties.class)
public class TwitchOAuthTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TwitchOAuthTokenProvider.class);
    private static final String CACHE_KEY = "oauth:twitch:token";
    private static final long REFRESH_BUFFER_SECONDS = 60L;
    private static final double REDIS_TTL_RATIO = 0.95; // expires_in의 95%만 Redis 캐시

    private final WebClient webClient;
    private final TwitchOAuthProperties props;
    private final RedisCacheTemplate cache;
    private final ReentrantLock refreshLock = new ReentrantLock();

    private volatile CachedToken inMemory;

    public TwitchOAuthTokenProvider(
            WebClient.Builder builder, TwitchOAuthProperties props, RedisCacheTemplate cache) {
        this.webClient = builder.clone().build(); // base URL 없이 — OAuth URL은 절대 경로
        this.props = props;
        this.cache = cache;
    }

    /**
     * 유효한 Bearer 토큰 반환. 필요 시 자동 갱신.
     *
     * @throws ExternalApiException.Server OAuth 호출 실패 시
     */
    public String getToken() {
        // 1. 인메모리 fast path
        CachedToken local = this.inMemory;
        if (local != null && !local.isExpiringSoon()) {
            return local.token;
        }

        // 2. Redis 검사
        Optional<CachedToken> redisCached = cache.get(CACHE_KEY, CachedToken.class);
        if (redisCached.isPresent() && !redisCached.get().isExpiringSoon()) {
            this.inMemory = redisCached.get();
            return redisCached.get().token;
        }

        // 3. 갱신 (락)
        refreshLock.lock();
        try {
            // 락 획득 후 다시 체크 (다른 스레드가 이미 갱신했을 수 있음)
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
            log.info("Twitch OAuth token refreshed, expires at {}", fresh.expiresAt);
            return fresh.token;
        } finally {
            refreshLock.unlock();
        }
    }

    /**
     * 테스트용 — 인메모리 캐시 무효화.
     */
    public void invalidate() {
        this.inMemory = null;
        cache.evict(CACHE_KEY);
    }

    private CachedToken fetchToken() {
        try {
            TwitchTokenResponse resp = webClient
                    .post()
                    .uri(props.oauthUrl())
                    .body(BodyInserters.fromFormData("client_id", props.clientId())
                            .with("client_secret", props.clientSecret())
                            .with("grant_type", "client_credentials"))
                    .retrieve()
                    .bodyToMono(TwitchTokenResponse.class)
                    .block();

            if (resp == null || resp.accessToken() == null) {
                throw new ExternalApiException.Server("twitch_oauth", "OAuth response empty", null);
            }
            Instant expiresAt = Instant.now().plusSeconds(resp.expiresIn());
            return new CachedToken(resp.accessToken(), expiresAt);
        } catch (ExternalApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalApiException.Server("twitch_oauth", "OAuth fetch failed: " + e.getMessage(), e);
        }
    }

    /**
     * 캐시된 토큰 (Jackson 역직렬화를 위해 public).
     */
    public record CachedToken(String token, Instant expiresAt) {
        public boolean isExpiringSoon() {
            return Instant.now().plusSeconds(REFRESH_BUFFER_SECONDS).isAfter(expiresAt);
        }
    }
}
