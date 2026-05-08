package com.gametrend.insight.application.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.gametrend.insight.application.stats.HitFlopClassifier.Classification;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HitFlopClassifierTest {

    private final HitFlopClassifier classifier = new HitFlopClassifier(new ZScoreNormalizer());

    @Test
    @DisplayName("classify 단건 — z > +1σ → HIT, z < -1σ → FLOP, 그 외 NORMAL")
    void classifySingle() {
        // mean=30, σ=10
        assertThat(classifier.classify(50, 30, 10, 1.0)).isEqualTo(Classification.HIT);   // z=2.0
        assertThat(classifier.classify(45, 30, 10, 1.0)).isEqualTo(Classification.HIT);   // z=1.5
        assertThat(classifier.classify(35, 30, 10, 1.0)).isEqualTo(Classification.NORMAL); // z=0.5
        assertThat(classifier.classify(15, 30, 10, 1.0)).isEqualTo(Classification.FLOP);  // z=-1.5
    }

    @Test
    @DisplayName("classifyAll 배치 — 5게임 CCU → HIT/NORMAL/FLOP 분류")
    void classifyBatch() {
        // CCU 분포 매우 편차 큼 (1게임이 압도적 HIT)
        Map<String, Double> games = new LinkedHashMap<>();
        games.put("CS2", 1_100_000.0);
        games.put("Dota2", 700_000.0);
        games.put("EldenRing", 200_000.0);
        games.put("Hades", 5_000.0);
        games.put("StardewValley", 80_000.0);

        var result = classifier.classifyAll(games, 1.0);

        assertThat(result).hasSize(5);
        // CS2 (1.1M)은 평균 대비 압도적 → HIT
        assertThat(result.get("CS2")).isEqualTo(Classification.HIT);
        // 나머지는 평균 미만 (CS2가 평균을 끌어올림) — 작은 게임들이 FLOP/NORMAL
        // 정확한 분류는 mean/stdDev 의존. 우리가 확인할 건 HIT 1개만 확실히.
    }

    @Test
    @DisplayName("threshold 변경 — 0.5σ 시 더 많은 HIT/FLOP")
    void thresholdSensitivity() {
        Map<String, Double> values = new LinkedHashMap<>();
        values.put("a", 10.0);
        values.put("b", 20.0);
        values.put("c", 30.0);
        values.put("d", 40.0);
        values.put("e", 50.0);

        var strict = classifier.classifyAll(values, 1.5);   // 엄격
        var loose = classifier.classifyAll(values, 0.5);    // 느슨

        long looseHits = loose.values().stream().filter(c -> c == Classification.HIT).count();
        long strictHits = strict.values().stream().filter(c -> c == Classification.HIT).count();
        // 느슨한 threshold면 HIT 후보가 더 많아짐
        assertThat(looseHits).isGreaterThanOrEqualTo(strictHits);
    }

    @Test
    @DisplayName("단일 표본 — 분산 0 → 모두 NORMAL")
    void singleSample() {
        Map<String, Double> single = Map.of("only", 100.0);
        var result = classifier.classifyAll(single, 1.0);
        assertThat(result.get("only")).isEqualTo(Classification.NORMAL);
    }

    @Test
    @DisplayName("countByClassification — 그룹 카운트")
    void counts() {
        Map<String, Classification> classified = Map.of(
                "a", Classification.HIT,
                "b", Classification.HIT,
                "c", Classification.NORMAL,
                "d", Classification.FLOP);

        var counts = classifier.countByClassification(classified);

        assertThat(counts.get(Classification.HIT)).isEqualTo(2L);
        assertThat(counts.get(Classification.NORMAL)).isEqualTo(1L);
        assertThat(counts.get(Classification.FLOP)).isEqualTo(1L);
    }

    @Test
    @DisplayName("빈 입력 → 빈 결과")
    void emptyInput() {
        assertThat(classifier.classifyAll(Map.of(), 1.0)).isEmpty();
        assertThat(classifier.classifyAll(null, 1.0)).isEmpty();
    }
}
