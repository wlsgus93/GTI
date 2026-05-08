package com.gametrend.insight.application.port.out;

import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import java.util.Optional;

/**
 * Steam Storefront API 포트 — 가격 + 디스카운트 조회.
 *
 * <p>인증 없는 공개 API. 응답은 {@code Map<appId-string, AppDetails>} 형태로 들어와서
 * 매퍼가 JsonNode로 풀어서 처리.
 */
public interface SteamStorefrontPort {

    /**
     * 게임의 현재 가격 스냅샷을 반환.
     *
     * @param gameId GTI DB의 게임 PK
     * @param appId  Steam appid
     * @return 응답이 있고 success=true면 PriceSnapshot, 아니면 empty
     */
    Optional<PriceSnapshot> fetchPrice(long gameId, long appId);
}
