package com.gametrend.insight.application.dimension.cluster;

import com.gametrend.insight.domain.dimension.cluster.GenreCluster;
import com.gametrend.insight.domain.dimension.cluster.GenreCluster.Cluster;
import com.gametrend.insight.domain.dimension.cluster.GenreCluster.ClusterMember;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.game.GenreJpaEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D4 산출 서비스 — Lloyd's KMeans 직접 구현 (Smile 의존성 X).
 *
 * <p>장르 one-hot vector + cosine distance + 반복 수렴.
 * 라벨링: 각 클러스터의 centroid 에서 weight 가장 높은 장르 2개 결합 (예: "RPG + Action").
 *
 * <p>한계 (V1): 장르 only — 메카닉 / Steam 태그 미포함. 향후 feature 확장 시 vocabulary 늘림.
 */
@Service
public class GenreClusterService {

    private static final Logger log = LoggerFactory.getLogger(GenreClusterService.class);

    private static final int MAX_ITER = 100;
    private static final double TOLERANCE = 1e-4;
    private static final long SEED = 42L; // reproducible

    private final GameJpaRepository gameRepo;

    public GenreClusterService(GameJpaRepository gameRepo) {
        this.gameRepo = gameRepo;
    }

    @Transactional(readOnly = true)
    public GenreCluster cluster(int k) {
        int safeK = (k < 2 || k > 20) ? 5 : k;
        List<GameJpaEntity> games = gameRepo.findAll();
        // 장르 없는 게임 제외
        List<GameJpaEntity> withGenres = games.stream()
                .filter(g -> g.getGenres() != null && !g.getGenres().isEmpty())
                .toList();

        if (withGenres.size() < safeK) {
            log.warn("D4 cluster: not enough games ({}) for k={}", withGenres.size(), safeK);
            return new GenreCluster(safeK, List.of(), 0.0, withGenres.size(), List.of(), Instant.now());
        }

        // 1. 장르 vocabulary 구성 (정렬된 unique 장르)
        Set<String> vocabSet = new TreeSet<>();
        withGenres.forEach(g -> g.getGenres().forEach(genre -> vocabSet.add(genre.getName())));
        List<String> vocab = new ArrayList<>(vocabSet);
        Map<String, Integer> vocabIndex = new HashMap<>();
        for (int i = 0; i < vocab.size(); i++) vocabIndex.put(vocab.get(i), i);

        // 2. 게임별 vector 구성 (L2-normalized)
        int dim = vocab.size();
        double[][] vectors = new double[withGenres.size()][dim];
        for (int i = 0; i < withGenres.size(); i++) {
            for (GenreJpaEntity ge : withGenres.get(i).getGenres()) {
                Integer idx = vocabIndex.get(ge.getName());
                if (idx != null) vectors[i][idx] = 1.0;
            }
            normalize(vectors[i]);
        }

        // 3. KMeans 초기화 — random k 게임을 centroid 로
        Random rng = new Random(SEED);
        double[][] centroids = new double[safeK][dim];
        Set<Integer> chosen = new TreeSet<>();
        while (chosen.size() < safeK) chosen.add(rng.nextInt(vectors.length));
        int ci = 0;
        for (int gi : chosen) {
            System.arraycopy(vectors[gi], 0, centroids[ci++], 0, dim);
        }

        // 4. 반복: assign + update
        int[] assignments = new int[vectors.length];
        for (int iter = 0; iter < MAX_ITER; iter++) {
            // assign — cosine distance 가 가장 낮은 centroid
            boolean changed = false;
            for (int i = 0; i < vectors.length; i++) {
                int best = 0;
                double bestDist = cosineDistance(vectors[i], centroids[0]);
                for (int c = 1; c < safeK; c++) {
                    double d = cosineDistance(vectors[i], centroids[c]);
                    if (d < bestDist) { bestDist = d; best = c; }
                }
                if (assignments[i] != best) { assignments[i] = best; changed = true; }
            }

            // update — cluster 평균
            double[][] newCentroids = new double[safeK][dim];
            int[] counts = new int[safeK];
            for (int i = 0; i < vectors.length; i++) {
                int c = assignments[i];
                for (int j = 0; j < dim; j++) newCentroids[c][j] += vectors[i][j];
                counts[c]++;
            }
            double maxShift = 0.0;
            for (int c = 0; c < safeK; c++) {
                if (counts[c] == 0) continue;
                for (int j = 0; j < dim; j++) newCentroids[c][j] /= counts[c];
                normalize(newCentroids[c]);
                double shift = euclidean(newCentroids[c], centroids[c]);
                if (shift > maxShift) maxShift = shift;
            }
            centroids = newCentroids;
            if (!changed || maxShift < TOLERANCE) {
                log.info("D4 KMeans converged iter={}, maxShift={}", iter, maxShift);
                break;
            }
        }

        // 5. silhouette score (단순 평균 cosine 거리 기반)
        double silhouette = computeSilhouette(vectors, assignments, safeK);

        // 6. 클러스터 record 빌드
        List<Cluster> result = buildClusters(safeK, vocab, vectors, centroids, assignments, withGenres);

        return new GenreCluster(safeK, result, silhouette, withGenres.size(), vocab, Instant.now());
    }

    private static void normalize(double[] v) {
        double norm = 0.0;
        for (double x : v) norm += x * x;
        if (norm > 0) {
            double s = Math.sqrt(norm);
            for (int i = 0; i < v.length; i++) v[i] /= s;
        }
    }

    /** cosine distance = 1 - cosine similarity (정규화된 벡터 가정). */
    private static double cosineDistance(double[] a, double[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) dot += a[i] * b[i];
        return 1.0 - dot;
    }

    private static double euclidean(double[] a, double[] b) {
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    private static double computeSilhouette(double[][] vectors, int[] assignments, int k) {
        // 단순 silhouette: 각 점의 (b - a) / max(a, b)
        // a = 같은 cluster 내 평균 거리, b = 가장 가까운 다른 cluster 평균 거리
        if (vectors.length < 2) return 0.0;
        double total = 0.0;
        int valid = 0;
        Map<Integer, List<Integer>> byCluster = new HashMap<>();
        for (int i = 0; i < assignments.length; i++) {
            byCluster.computeIfAbsent(assignments[i], x -> new ArrayList<>()).add(i);
        }
        for (int i = 0; i < vectors.length; i++) {
            int own = assignments[i];
            List<Integer> ownMembers = byCluster.get(own);
            if (ownMembers == null || ownMembers.size() < 2) continue;
            double a = avgDistance(vectors, i, ownMembers);
            double b = Double.POSITIVE_INFINITY;
            for (Map.Entry<Integer, List<Integer>> e : byCluster.entrySet()) {
                if (e.getKey() == own) continue;
                double d = avgDistance(vectors, i, e.getValue());
                if (d < b) b = d;
            }
            if (Double.isInfinite(b)) continue;
            double sil = (b - a) / Math.max(a, b);
            total += sil;
            valid++;
        }
        return valid == 0 ? 0.0 : total / valid;
    }

    private static double avgDistance(double[][] vectors, int idx, List<Integer> others) {
        double sum = 0.0;
        int count = 0;
        for (int j : others) {
            if (j == idx) continue;
            sum += cosineDistance(vectors[idx], vectors[j]);
            count++;
        }
        return count == 0 ? 0.0 : sum / count;
    }

    private static List<Cluster> buildClusters(int k, List<String> vocab,
            double[][] vectors, double[][] centroids, int[] assignments,
            List<GameJpaEntity> games) {
        List<Cluster> result = new ArrayList<>();
        for (int c = 0; c < k; c++) {
            // centroid → genre weight Map (Top 5)
            Map<String, Double> centroidWeights = new LinkedHashMap<>();
            List<int[]> indexed = new ArrayList<>();
            for (int j = 0; j < vocab.size(); j++) {
                if (centroids[c][j] > 0.01) indexed.add(new int[]{j, (int) (centroids[c][j] * 10000)});
            }
            indexed.sort((a, b) -> Integer.compare(b[1], a[1]));
            for (int i = 0; i < Math.min(5, indexed.size()); i++) {
                int gIdx = indexed.get(i)[0];
                centroidWeights.put(vocab.get(gIdx), centroids[c][gIdx]);
            }

            // members
            List<ClusterMember> members = new ArrayList<>();
            for (int i = 0; i < assignments.length; i++) {
                if (assignments[i] != c) continue;
                double dist = cosineDistance(vectors[i], centroids[c]);
                List<String> gameGenres = games.get(i).getGenres().stream()
                        .map(GenreJpaEntity::getName).sorted().toList();
                members.add(new ClusterMember(games.get(i).getId(), games.get(i).getName(),
                        gameGenres, dist));
            }
            members.sort(Comparator.comparingDouble(ClusterMember::distanceToCentroid));

            // 라벨 — top 2 장르 결합
            String label = centroidWeights.keySet().stream().limit(2)
                    .reduce((a, b) -> a + " + " + b).orElse("Cluster " + c);

            result.add(new Cluster(c, label, centroidWeights, members, members.size()));
        }
        result.sort(Comparator.comparingInt(Cluster::size).reversed());
        return result;
    }
}
