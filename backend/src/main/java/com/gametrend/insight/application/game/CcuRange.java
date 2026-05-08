package com.gametrend.insight.application.game;

import java.time.Duration;
import java.util.Locale;

/**
 * CCU 시계열 조회 범위. 프리셋만 허용 (자유 from/to는 W2 Day 3+).
 *
 * <p>UI 가독성: 24h / 7d / 30d / 90d 4가지 프리셋 제공.
 */
public enum CcuRange {
    HOURS_24(Duration.ofHours(24), "24h"),
    DAYS_7(Duration.ofDays(7), "7d"),
    DAYS_30(Duration.ofDays(30), "30d"),
    DAYS_90(Duration.ofDays(90), "90d");

    private final Duration duration;
    private final String code;

    CcuRange(Duration duration, String code) {
        this.duration = duration;
        this.code = code;
    }

    public Duration duration() {
        return duration;
    }

    public String code() {
        return code;
    }

    /**
     * 쿼리 파라미터 문자열 → enum.
     *
     * @throws IllegalArgumentException 인식 불가 코드 (컨트롤러에서 400 매핑)
     */
    public static CcuRange parse(String code) {
        if (code == null || code.isBlank()) {
            return DAYS_30;
        }
        String normalized = code.trim().toLowerCase(Locale.ROOT);
        for (CcuRange r : values()) {
            if (r.code.equals(normalized)) {
                return r;
            }
        }
        throw new IllegalArgumentException("unsupported range: " + code + " (allowed: 24h, 7d, 30d, 90d)");
    }
}
