package com.gametrend.insight.domain.snapshot;

import java.time.Instant;

/**
 * 시점별 가격 스냅샷. 정수 cents 단위로 저장 (부동소수 회피).
 *
 * @param priceCents       센트 단위 가격 (예: $9.99 → 999)
 * @param discountPercent  할인율 0~100
 * @param stale            라이브 호출 실패로 캐시 fallback일 때 true
 */
public record PriceSnapshot(
        Long id,
        Long gameId,
        String currency,
        Long priceCents,
        int discountPercent,
        Instant capturedAt,
        SnapshotSource source,
        boolean stale) {

    public PriceSnapshot {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be 3-char ISO code");
        }
        if (priceCents == null || priceCents < 0) {
            throw new IllegalArgumentException("priceCents must be non-negative");
        }
        if (discountPercent < 0 || discountPercent > 100) {
            throw new IllegalArgumentException("discountPercent must be 0..100");
        }
    }
}
