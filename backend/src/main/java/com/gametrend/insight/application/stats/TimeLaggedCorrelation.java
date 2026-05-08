package com.gametrend.insight.application.stats;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.springframework.stereotype.Component;

/**
 * 시차 상관 (Time-lagged Correlation) — Pearson + lag window. 룰 ecc/data-analyst Tier 1.
 *
 * <p>적용: 마케터 페르소나 — "캠페인 t+2일 후 CCU r=0.82" 같은 마케팅 인과 추정.
 *
 * <p>한계 (정직):
 * <ul>
 *   <li>**상관 ≠ 인과** — 시차 상관 높아도 다른 요인 가능 (계절성, 동시 이벤트 등).
 *       정확한 인과는 RCT(A/B 테스트) 또는 격리 실험 필요
 *   <li>**Pearson은 선형 가정** — 비선형 관계는 못 잡음 (Spearman 같은 rank correlation 대안)
 *   <li>**노이즈 민감** — 짧은 시계열엔 우연 상관 (spurious correlation) 위험. min sample 5 강제
 * </ul>
 */
@Component
public class TimeLaggedCorrelation {

    /** 통계적 의미를 위한 최소 sample size — n &lt; 5면 결과 무의미. */
    public static final int MIN_SAMPLE_SIZE = 5;

    /**
     * X(t)와 Y(t+lag)의 Pearson 상관계수.
     *
     * <p>예: x=캠페인 클릭수 일별, y=CCU 일별, lag=2 → "클릭 후 2일 후 CCU 영향".
     *
     * @param x   시계열 X (시간순 정렬)
     * @param y   시계열 Y — x와 같은 길이
     * @param lag X에 대한 Y의 시간 지연 (lag &ge; 0)
     * @return Pearson r (-1 ~ +1) 또는 sample 부족 시 null
     * @throws IllegalArgumentException x와 y 길이 다름 / lag &lt; 0
     */
    public Double correlation(double[] x, double[] y, int lag) {
        if (x == null || y == null) return null;
        if (x.length != y.length) {
            throw new IllegalArgumentException(
                    "x and y must have same length, got " + x.length + " vs " + y.length);
        }
        if (lag < 0) {
            throw new IllegalArgumentException("lag must be >= 0, got " + lag);
        }

        int n = x.length - lag;
        if (n < MIN_SAMPLE_SIZE) return null;

        double[] xLagged = Arrays.copyOfRange(x, 0, n);
        double[] yLagged = Arrays.copyOfRange(y, lag, y.length);

        // Apache Commons Math: Pearson correlation
        // 분산 0 (모든 값 동일) 케이스는 NaN 반환 — null로 정규화
        double r = new PearsonsCorrelation().correlation(xLagged, yLagged);
        return Double.isNaN(r) ? null : r;
    }

    /**
     * 0 ~ maxLag 범위에서 |r| 가장 큰 lag 검출.
     *
     * <p>"X가 Y에 가장 강하게 영향 미치는 시점이 며칠 후?" — 자동 발견.
     *
     * @return null이면 모든 lag에서 sample 부족 또는 분산 0
     */
    public BestLagResult findBestLag(double[] x, double[] y, int maxLag) {
        Map<Integer, Double> correlations = new LinkedHashMap<>();
        Integer bestLag = null;
        double bestCorr = 0.0;

        for (int lag = 0; lag <= maxLag; lag++) {
            Double r = correlation(x, y, lag);
            if (r != null) {
                correlations.put(lag, r);
                if (bestLag == null || Math.abs(r) > Math.abs(bestCorr)) {
                    bestLag = lag;
                    bestCorr = r;
                }
            }
        }

        if (bestLag == null) return null;
        return new BestLagResult(bestLag, bestCorr, correlations);
    }

    /**
     * @param bestLag           최적 lag (일)
     * @param bestCorrelation   해당 lag에서 r
     * @param correlationsByLag 모든 lag별 r (LinkedHashMap — 순서 유지)
     */
    public record BestLagResult(
            int bestLag,
            double bestCorrelation,
            Map<Integer, Double> correlationsByLag) {}
}
