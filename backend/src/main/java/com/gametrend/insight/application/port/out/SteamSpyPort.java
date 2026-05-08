package com.gametrend.insight.application.port.out;

import java.util.Optional;

/**
 * SteamSpy 비공식 API — 판매량 추정, 플레이타임 등.
 *
 * <p>레이트 리밋: 1 req/sec (24h 캐시로 완화). 응답 정확도는 추정치.
 */
public interface SteamSpyPort {

    Optional<SteamSpyEstimates> fetchEstimates(long gameId, long steamAppId);

    record SteamSpyEstimates(
            long appId,
            String name,
            String ownersRange, // 예: "5,000,000 .. 10,000,000"
            Integer averagePlaytimeForever,
            Integer median2Weeks,
            Integer ccu) {}
}
