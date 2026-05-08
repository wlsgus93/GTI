package com.gametrend.insight.domain.snapshot;

import java.time.Instant;

/**
 * 시점별 시청자 스냅샷 (Twitch, YouTube).
 */
public record ViewerSnapshot(
        Long id,
        Long gameId,
        int concurrentViewers,
        Instant capturedAt,
        SnapshotSource source,
        boolean stale) {

    public ViewerSnapshot {
        if (concurrentViewers < 0) {
            throw new IllegalArgumentException("concurrentViewers must be non-negative");
        }
    }
}
