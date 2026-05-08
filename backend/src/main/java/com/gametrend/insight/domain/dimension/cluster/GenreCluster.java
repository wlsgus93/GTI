package com.gametrend.insight.domain.dimension.cluster;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * D4 — 장르 클러스터 (Genre Cluster).
 *
 * <p>입력: 게임 + 장르 매핑 (game_genres). 향후 IGDB 메카닉 / Steam 태그 추가 예정.
 * 산출: KMeans 클러스터 + 각 클러스터의 centroid (장르 weight) + 멤버 게임 + 라벨 + silhouette score.
 *
 * <p>알고리즘 (LLM X — Lloyd's KMeans 직접 구현):
 * <ol>
 *   <li>장르 one-hot encoding (game × genre 매트릭스)
 *   <li>K-means initialize: random k 게임 vector
 *   <li>반복: assign + update centroid until convergence (max 100 iter, tolerance 1e-4)
 *   <li>distance: cosine similarity (sparse vector 정합)
 *   <li>silhouette score 산출 (cluster 응집도)
 * </ol>
 */
public record GenreCluster(
        int k,
        List<Cluster> clusters,
        double silhouetteScore,
        int totalGames,
        List<String> genreVocabulary,
        Instant generatedAt) {

    public record Cluster(
            int id,
            String label,
            Map<String, Double> centroidWeights, // genre name → weight
            List<ClusterMember> members,
            int size) {}

    public record ClusterMember(
            long gameId,
            String name,
            List<String> genres,
            double distanceToCentroid) {}
}
