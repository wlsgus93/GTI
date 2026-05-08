package com.gametrend.insight.infrastructure.external.steamstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import java.time.Instant;
import java.util.Optional;

/**
 * Steam Storefront {@code /appdetails} 응답 → PriceSnapshot 매핑.
 *
 * <p>응답은 {@code {"<appid>": {"success": true, "data": {...}}}} 형태.
 * 무료 게임은 {@code price_overview}가 없을 수 있으므로 별도 처리.
 */
public final class SteamStorefrontMapper {

    private SteamStorefrontMapper() {}

    public static Optional<PriceSnapshot> toPriceSnapshot(JsonNode root, long appId, long gameId) {
        if (root == null) {
            return Optional.empty();
        }
        JsonNode item = root.get(String.valueOf(appId));
        if (item == null || !item.path("success").asBoolean(false)) {
            return Optional.empty();
        }
        JsonNode data = item.get("data");
        if (data == null) {
            return Optional.empty();
        }

        JsonNode po = data.get("price_overview");
        // 무료 게임: price_overview 없음 → priceCents=0
        if (po == null) {
            boolean isFree = data.path("is_free").asBoolean(false);
            if (!isFree) {
                return Optional.empty(); // 가격 정보 없고 무료도 아니면 매핑 불가
            }
            return Optional.of(new PriceSnapshot(
                    null, gameId, "USD", 0L, 0, Instant.now(), SnapshotSource.STEAM_STORE, false));
        }

        String currency = po.path("currency").asText("USD");
        long priceCents = po.path("final").asLong(0L);
        int discountPercent = po.path("discount_percent").asInt(0);

        return Optional.of(new PriceSnapshot(
                null,
                gameId,
                currency,
                priceCents,
                Math.max(0, Math.min(100, discountPercent)),
                Instant.now(),
                SnapshotSource.STEAM_STORE,
                false));
    }
}
