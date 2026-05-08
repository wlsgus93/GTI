package com.gametrend.insight.application.economics;

/**
 * 매출 추정 신뢰도. SteamSpy owners 범위 폭 + 데이터 신선도에 의존.
 *
 * <ul>
 *   <li>HIGH — owners 상대폭 < 30%, 가격 있음, 스냅샷 24h 이내
 *   <li>MEDIUM — owners 상대폭 < 60%, 가격 있음
 *   <li>LOW — 그 외 (owners 폭 큰 경우 / 가격 없음 / 스냅샷 stale)
 * </ul>
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}
