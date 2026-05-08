package com.gametrend.insight.application.watchlist;

import java.time.Instant;

/**
 * P4 워치리스트 응답 항목 — 게임 메타 (latestCcu 포함, dashboard 친화적).
 */
public record WatchlistItem(
        Long id,
        Long gameId,
        Long steamAppId,
        String name,
        String coverImageUrl,
        Integer latestCcu,
        String note,
        Instant addedAt) {
}
