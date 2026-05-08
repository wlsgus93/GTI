package com.gametrend.insight.domain.snapshot;

/**
 * 9개 외부 데이터 소스 식별자. DB에는 VARCHAR(20)로 저장.
 * 자세한 명세: docs/data-sources.md
 */
public enum SnapshotSource {
    STEAM,           // Steam Web API
    STEAM_STORE,     // Steam Storefront (price/reviews)
    STEAM_DB,        // SteamDB (W2 별도 마이크로서비스)
    STEAM_SPY,       // SteamSpy (sales estimates)
    IGDB,            // IGDB metadata
    TWITCH,          // Twitch viewers
    YOUTUBE,         // YouTube search/popularity
    OPENCRITIC,      // OpenCritic scores
    REDDIT,          // Reddit mentions
    APPLE,           // Apple Top Charts (RSS)
    GOOGLE_PLAY      // Google Play (W2 별도 마이크로서비스)
}
