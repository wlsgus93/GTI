package com.gametrend.insight.application.ingestion;

import com.gametrend.insight.application.port.out.SteamSpyPort.SteamSpyEstimates;
import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import java.time.Instant;

/**
 * SteamSpyEstimates → PlayerSnapshot 변환.
 *
 * <p>SteamSpy의 owners 범위 string ("5,000,000 .. 10,000,000")을 PlayerSnapshot의
 * ownersLow/ownersHigh로 파싱. ccu가 응답에 있으면 concurrentPlayers에 매핑.
 *
 * <p>한계 (룰 safety.md "Range" 등급):
 * <ul>
 *   <li>SteamSpy owners는 추정치 — ±오차 가능. 운영 분석 시 confidence MEDIUM 표기 권장.
 *   <li>파싱 실패 시 null 반환 (호출자가 skip).
 * </ul>
 */
public final class SteamSpyMapper {

    private SteamSpyMapper() {}

    /**
     * @param est       SteamSpy 응답
     * @param gameId    GTI DB의 게임 PK
     * @param capturedAt 캡처 시각 (보통 Instant.now())
     * @return PlayerSnapshot 또는 owners 파싱 실패 시 null
     */
    public static PlayerSnapshot toPlayerSnapshot(
            SteamSpyEstimates est, long gameId, Instant capturedAt) {
        if (est == null) return null;

        Long[] range = parseOwnersRange(est.ownersRange());
        Long ownersLow = range != null ? range[0] : null;
        Long ownersHigh = range != null ? range[1] : null;

        // owners도 없고 ccu도 없으면 데이터 없음 — null 반환
        if (ownersLow == null && est.ccu() == null) return null;

        return new PlayerSnapshot(
                null,
                gameId,
                est.ccu(),
                null,
                null,
                ownersLow,
                ownersHigh,
                capturedAt,
                SnapshotSource.STEAM_SPY,
                false);
    }

    /**
     * "5,000,000 .. 10,000,000" → [5000000, 10000000].
     * 형식 이상 시 null 반환 (silent fallback — 호출자가 skip 결정).
     */
    static Long[] parseOwnersRange(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // ".." 또는 " .. " 분리
        String[] parts = raw.split("\\.\\.");
        if (parts.length != 2) return null;
        try {
            long low = Long.parseLong(parts[0].trim().replace(",", ""));
            long high = Long.parseLong(parts[1].trim().replace(",", ""));
            if (low > high) return null;
            return new Long[]{low, high};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
