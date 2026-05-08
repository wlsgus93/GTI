package com.gametrend.insight.application.stats;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Z-score 기반 Hit/Normal/Flop 분류기 — 룰 Tier 1.
 *
 * <p>분류 기준 (default threshold = 1.0σ):
 * <ul>
 *   <li>z > +threshold → HIT (그룹 평균 대비 우수)
 *   <li>z < -threshold → FLOP (그룹 평균 대비 부진)
 *   <li>그 외 → NORMAL
 * </ul>
 *
 * <p>적용: D1 출시 동향 — 장르 평균 대비 흥행/실패 게임 자동 분류.
 *
 * <p>한계: 정규분포 가정. 멱법칙 분포 (CCU 같은 거대 편차)엔 log 변환 후 적용 권고.
 */
@Component
public class HitFlopClassifier {

    public static final double DEFAULT_THRESHOLD = 1.0;

    public enum Classification {
        HIT,
        NORMAL,
        FLOP
    }

    private final ZScoreNormalizer normalizer;

    public HitFlopClassifier(ZScoreNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    /** 단일 값 분류 (mean/stdDev 미리 알 때). */
    public Classification classify(double value, double mean, double stdDev, double threshold) {
        double z = normalizer.normalize(value, mean, stdDev);
        if (z > threshold) return Classification.HIT;
        if (z < -threshold) return Classification.FLOP;
        return Classification.NORMAL;
    }

    /** 배치 분류 — 키별 값 → 키별 분류. 입력 순서 유지 (LinkedHashMap). */
    public <K> Map<K, Classification> classifyAll(Map<K, Double> values, double threshold) {
        if (values == null || values.isEmpty()) return Map.of();
        if (values.size() == 1) {
            // 단일 표본은 분산 X → 모두 NORMAL
            Map<K, Classification> result = new LinkedHashMap<>();
            values.keySet().forEach(k -> result.put(k, Classification.NORMAL));
            return result;
        }

        double[] arr = values.values().stream().mapToDouble(Double::doubleValue).toArray();
        var stats = normalizer.normalizeWithStats(arr);

        Map<K, Classification> result = new LinkedHashMap<>();
        int i = 0;
        for (K key : values.keySet()) {
            double z = stats.zScores()[i++];
            Classification c = z > threshold ? Classification.HIT
                    : z < -threshold ? Classification.FLOP
                    : Classification.NORMAL;
            result.put(key, c);
        }
        return result;
    }

    /** 그룹별 카운트 — { HIT: 3, NORMAL: 5, FLOP: 2 }. */
    public Map<Classification, Long> countByClassification(Map<?, Classification> classified) {
        Map<Classification, Long> counts = new LinkedHashMap<>();
        for (Classification c : Classification.values()) counts.put(c, 0L);
        classified.values().forEach(c -> counts.merge(c, 1L, Long::sum));
        return counts;
    }
}
