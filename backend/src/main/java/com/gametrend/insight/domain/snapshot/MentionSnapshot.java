package com.gametrend.insight.domain.snapshot;

import java.time.Instant;

/**
 * 시점별 커뮤니티 멘션 스냅샷 (Reddit, Discord 등).
 *
 * @param sentiment   POS / NEU / NEG (nullable)
 */
public record MentionSnapshot(
        Long id,
        Long gameId,
        int mentionCount,
        Sentiment sentiment,
        Instant capturedAt,
        SnapshotSource source,
        boolean stale) {

    public enum Sentiment {
        POS, NEU, NEG
    }
}
