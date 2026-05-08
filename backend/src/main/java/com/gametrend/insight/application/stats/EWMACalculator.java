package com.gametrend.insight.application.stats;

import org.springframework.stereotype.Component;

/**
 * 지수가중이동평균 (Exponentially Weighted Moving Average) — 룰 Tier 1.
 *
 * <p>공식: {@code EMA_t = α·x_t + (1-α)·EMA_{t-1}}, EMA_0 = x_0
 *
 * <p>α (0 < α < 1):
 * <ul>
 *   <li>α 큼 (예: 0.5+) — 최근값 강조, 변동에 민감
 *   <li>α 작음 (예: 0.1) — 과거 평균 가중, 부드러움
 *   <li>기본값 0.3 — 균형
 * </ul>
 *
 * <p>적용: TrendScore 모멘텀 ("지난주 대비 상승폭"), CCU 단기 추세 평활.
 */
@Component
public class EWMACalculator {

    public static final double DEFAULT_ALPHA = 0.3;

    /** 시계열 EWMA 전체 계산 (시간순 정렬: 오래된 → 최신). */
    public double[] calculate(double[] values, double alpha) {
        validateAlpha(alpha);
        if (values == null || values.length == 0) return new double[0];

        double[] result = new double[values.length];
        result[0] = values[0]; // EMA_0 = x_0
        for (int i = 1; i < values.length; i++) {
            result[i] = alpha * values[i] + (1.0 - alpha) * result[i - 1];
        }
        return result;
    }

    /** 가장 최근 EWMA 값 (현재 모멘텀 지표). */
    public double current(double[] values, double alpha) {
        if (values == null || values.length == 0) return 0.0;
        double[] all = calculate(values, alpha);
        return all[all.length - 1];
    }

    /**
     * 모멘텀 변화율 (%) — 현재 EWMA vs lookback 시점 EWMA.
     *
     * <p>예: 30일 시계열에서 momentum(values, 0.3, 7) = 7일 전 EWMA 대비 현재 EWMA 변화율.
     *
     * @return 변화율 (%) 또는 lookback 시점 값 0이면 null
     */
    public Double momentum(double[] values, double alpha, int lookback) {
        if (values == null || values.length <= lookback) return null;
        double[] ema = calculate(values, alpha);
        double current = ema[ema.length - 1];
        double past = ema[ema.length - 1 - lookback];
        if (past == 0.0) return null;
        return ((current - past) / past) * 100.0;
    }

    private static void validateAlpha(double alpha) {
        if (alpha <= 0.0 || alpha >= 1.0) {
            throw new IllegalArgumentException("alpha must be in (0, 1), got " + alpha);
        }
    }
}
