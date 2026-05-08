package com.gametrend.insight.infrastructure.external.reddit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reddit API 설정.
 *
 * <p><b>두 가지 모드</b>:
 * <ul>
 *   <li><b>OAuth Script App</b> ({@code useOauth=true}): {@code oauth.reddit.com} 호출,
 *       Authorization Bearer + 100 req/min. Reddit Builder Policy 검토 통과 + script type app 등록 필요.
 *   <li><b>Anonymous Fetch</b> ({@code useOauth=false}, default): {@code www.reddit.com/search.json}
 *       직접 호출, User-Agent 헤더만, 60 req/10min. Builder Policy 통과 대기 시 즉시 가능.
 * </ul>
 *
 * <p>두 모드 모두 명시적 User-Agent 필수 (예: {@code gti-game-trend-insight/1.0 (by /u/username)} —
 * 일반 브라우저 UA 또는 빈 UA = 영구 429).
 *
 * @param baseUrl          OAuth base URL (예: {@code https://oauth.reddit.com})
 * @param anonymousBaseUrl Anonymous base URL (예: {@code https://www.reddit.com})
 * @param oauthUrl         OAuth token endpoint (예: {@code https://www.reddit.com/api/v1/access_token})
 * @param clientId         Script App Client ID
 * @param clientSecret     Script App Client Secret
 * @param userAgent        User-Agent 헤더 (필수)
 * @param useOauth         true = OAuth, false = anonymous fetch
 */
@ConfigurationProperties(prefix = "gti.external.reddit")
public record RedditProperties(
        String baseUrl,
        String anonymousBaseUrl,
        String oauthUrl,
        String clientId,
        String clientSecret,
        String userAgent,
        boolean useOauth) {

    public RedditProperties {
        if (anonymousBaseUrl == null || anonymousBaseUrl.isBlank()) {
            anonymousBaseUrl = "https://www.reddit.com";
        }
    }
}
