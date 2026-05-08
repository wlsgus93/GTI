package com.gametrend.insight.domain.snapshot;

import java.time.Instant;

/**
 * 시점별 플레이어/리뷰/소유자 스냅샷.
 *
 * <p>소스별로 채우는 필드가 다름:
 * <ul>
 *   <li>STEAM (Web API): concurrentPlayers
 *   <li>STEAM_STORE (Storefront): reviewScorePositive/Total
 *   <li>STEAM_SPY: ownersLow/High (범위)
 * </ul>
 *
 * @param concurrentPlayers   동시접속자 수 (nullable)
 * @param reviewScorePositive 긍정 리뷰 수 (nullable)
 * @param reviewScoreTotal    전체 리뷰 수 (nullable)
 * @param ownersLow           소유자 범위 하한 (nullable, SteamSpy)
 * @param ownersHigh          소유자 범위 상한 (nullable, SteamSpy)
 */
public record PlayerSnapshot(
        Long id,
        Long gameId,
        Integer concurrentPlayers,
        Integer reviewScorePositive,
        Integer reviewScoreTotal,
        Long ownersLow,
        Long ownersHigh,
        Instant capturedAt,
        SnapshotSource source,
        boolean stale) {
}
