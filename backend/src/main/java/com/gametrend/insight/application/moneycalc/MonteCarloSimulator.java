package com.gametrend.insight.application.moneycalc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Monte Carlo 시뮬레이션 — Triangular distribution sampling.
 *
 * <p>왜 triangular?
 * <ul>
 *   <li>3-pt 추정 (비관/보통/낙관)에 자연스럽게 fit
 *   <li>PERT(베타) 보다 단순, 의사결정 입력엔 충분
 *   <li>역변환 sampling으로 closed-form, deterministic seed 가능
 * </ul>
 *
 * <p>Hot loop은 double로 (BigDecimal 약 100배 느림). 결과 통계만 BigDecimal.
 */
@Component
public class MonteCarloSimulator {

    private final MoneyCalcCalculator calculator;

    public MonteCarloSimulator(MoneyCalcCalculator calculator) {
        this.calculator = calculator;
    }

    public MoneyCalcResult.MonteCarloOutput simulate(MoneyCalcRequest req, long seed) {
        int iterations = req.iterationsOrDefault();
        Random rnd = new Random(seed);

        double pessOwners = req.pessimistic().owners();
        double realOwners = req.realistic().owners();
        double optOwners = req.optimistic().owners();
        double pessPrice = req.pessimistic().priceCents() / 100.0;
        double realPrice = req.realistic().priceCents() / 100.0;
        double optPrice = req.optimistic().priceCents() / 100.0;
        double totalCost = req.totalCostCents() / 100.0;

        double[] netSamples = new double[iterations];
        int profitableCount = 0;

        for (int i = 0; i < iterations; i++) {
            double owners = sampleTriangular(pessOwners, realOwners, optOwners, rnd);
            double price = sampleTriangular(pessPrice, realPrice, optPrice, rnd);
            double net = calculator.iterateNetUsd(owners, price);
            netSamples[i] = net;
            if (net - totalCost > 0) profitableCount++;
        }

        Arrays.sort(netSamples);
        double p10 = percentile(netSamples, 10);
        double p50 = percentile(netSamples, 50);
        double p90 = percentile(netSamples, 90);
        double mean = mean(netSamples);
        double stdDev = stdDev(netSamples, mean);
        double profitProbPct = (double) profitableCount / iterations * 100.0;

        return new MoneyCalcResult.MonteCarloOutput(
                iterations,
                round2(p10),
                round2(p50),
                round2(p90),
                round2(mean),
                round2(stdDev),
                Math.round(profitProbPct * 10.0) / 10.0); // 1 자리
    }

    /**
     * Triangular distribution inverse CDF sampling.
     *
     * <p>특수 케이스:
     * <ul>
     *   <li>min == max: 항상 min 반환 (degenerate)
     *   <li>mode == min: 단순 우삼각 → 좌변 케이스 안 들어옴
     * </ul>
     */
    static double sampleTriangular(double min, double mode, double max, Random rnd) {
        if (max <= min) return min;
        double u = rnd.nextDouble();
        double f = (mode - min) / (max - min);
        if (u < f) {
            return min + Math.sqrt(u * (max - min) * (mode - min));
        } else {
            return max - Math.sqrt((1.0 - u) * (max - min) * (max - mode));
        }
    }

    private static double percentile(double[] sorted, int p) {
        if (sorted.length == 0) return 0;
        double rank = (p / 100.0) * (sorted.length - 1);
        int lo = (int) Math.floor(rank);
        int hi = (int) Math.ceil(rank);
        if (lo == hi) return sorted[lo];
        double frac = rank - lo;
        return sorted[lo] + frac * (sorted[hi] - sorted[lo]);
    }

    private static double mean(double[] arr) {
        double sum = 0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private static double stdDev(double[] arr, double mean) {
        double sumSq = 0;
        for (double v : arr) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / arr.length);
    }

    private static BigDecimal round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
}
