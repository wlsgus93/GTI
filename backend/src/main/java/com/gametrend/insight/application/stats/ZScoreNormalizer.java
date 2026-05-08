package com.gametrend.insight.application.stats;

import org.springframework.stereotype.Component;

/**
 * Z-score 정규화 — `z = (x - μ) / σ` (룰 ecc/data-analyst/methods-catalog Tier 1).
 *
 * <p>9 소스 통합 점수의 핵심 — 다른 단위(USD vs CCU vs viewers)를 동일 척도로.
 *
 * <p>stdDev=0 가드: 모든 값이 동일하면 분산 0 → 분모 0. 이 경우 모든 z-score = 0 반환.
 */
@Component
public class ZScoreNormalizer {

    /** 단일 값 정규화 (mean, stdDev 미리 알 때). */
    public double normalize(double value, double mean, double stdDev) {
        if (stdDev == 0.0) return 0.0;
        return (value - mean) / stdDev;
    }

    /** 배열 정규화 — mean/stdDev 자체 계산 후 모든 값을 z-score로. */
    public double[] normalizeAll(double[] values) {
        if (values == null || values.length == 0) return new double[0];
        double mean = mean(values);
        double std = stdDev(values, mean);
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = normalize(values[i], mean, std);
        }
        return result;
    }

    /** mean/stdDev 함께 반환 — 후속 분류기(HitFlop)에 재사용. */
    public NormalizedResult normalizeWithStats(double[] values) {
        if (values == null || values.length == 0) {
            return new NormalizedResult(new double[0], 0.0, 0.0);
        }
        double mean = mean(values);
        double std = stdDev(values, mean);
        double[] zScores = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            zScores[i] = normalize(values[i], mean, std);
        }
        return new NormalizedResult(zScores, mean, std);
    }

    public static double mean(double[] values) {
        double sum = 0;
        for (double v : values) sum += v;
        return sum / values.length;
    }

    public static double stdDev(double[] values, double mean) {
        double sumSq = 0;
        for (double v : values) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / values.length);
    }

    public record NormalizedResult(double[] zScores, double mean, double stdDev) {}
}
