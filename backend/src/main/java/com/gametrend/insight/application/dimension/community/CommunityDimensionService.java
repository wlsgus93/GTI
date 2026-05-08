package com.gametrend.insight.application.dimension.community;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.stats.ZScoreNormalizer;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D5 커뮤니티 활성도 차원 v1 — mention_snapshot 집계 + Z-score 활성도 + sentiment 분포.
 *
 * <p>v1 한계 (W7+ 보강):
 * <ul>
 *   <li>Pain Point 추출 — mention 텍스트 컬럼 부재로 V1은 빈 리스트
 *   <li>LLM Sentiment 직접 호출 X — 이미 sentiment 컬럼에 분류된 데이터 집계
 *   <li>EWMA 모멘텀 — 시계열 데이터 부족 (시드는 1 시점만)
 *   <li>DBSCAN 스팸 분리 — 텍스트 임베딩 데이터 부재로 미적용
 * </ul>
 *
 * <p>Pain Point 통합 + 시계열 모멘텀 + DBSCAN 클러스터링은 별도 구현 (W7+).
 */
@Service
public class CommunityDimensionService {

    public static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String CACHE_PREFIX = "dim:d5:community:";

    private final GameJpaRepository gameRepo;
    private final MentionSnapshotJpaRepository mentionRepo;
    private final RedisCacheTemplate redisCache;
    private final ZScoreNormalizer normalizer;

    public CommunityDimensionService(
            GameJpaRepository gameRepo,
            MentionSnapshotJpaRepository mentionRepo,
            RedisCacheTemplate redisCache,
            ZScoreNormalizer normalizer) {
        this.gameRepo = gameRepo;
        this.mentionRepo = mentionRepo;
        this.redisCache = redisCache;
        this.normalizer = normalizer;
    }

    @Transactional(readOnly = true)
    public CommunityDimension getCommunityDimension(long gameId) {
        // L1 Redis cache
        String cacheKey = CACHE_PREFIX + gameId;
        Optional<CommunityDimension> hot = redisCache.get(cacheKey, CommunityDimension.class);
        if (hot.isPresent()) return hot.get();

        // 게임 존재 검증
        GameJpaEntity game = gameRepo.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        // mention_snapshot 데이터 — 게임의 모든 (source, sentiment) 조합
        List<MentionSnapshotJpaEntity> mentions = mentionRepo.findByGameId(gameId);

        // 1. 플랫폼별 + sentiment 별 집계
        Map<SnapshotSource, Long> mentionsByPlatform = new EnumMap<>(SnapshotSource.class);
        long pos = 0, neu = 0, neg = 0;
        long total = 0;

        for (MentionSnapshotJpaEntity m : mentions) {
            int count = m.getMentionCount();
            total += count;
            mentionsByPlatform.merge(m.getSource(), (long) count, Long::sum);

            if (m.getSentiment() != null) {
                switch (m.getSentiment()) {
                    case POS -> pos += count;
                    case NEU -> neu += count;
                    case NEG -> neg += count;
                }
            }
        }

        var sentiment = CommunityDimension.SentimentBreakdown.of(pos, neu, neg);

        // 2. 활성도 점수 — 전체 게임 mention 평균 대비 Z-score
        Double activityZ = computeActivityZScore(gameId, total);
        String activityClass = classifyActivity(activityZ);

        // 3. 신뢰도 — mention 데이터 양 + sentiment 분류 비율
        Confidence confidence = assessConfidence(total, pos + neu + neg);

        // 4. Pain Points — V1 빈 리스트 (텍스트 컬럼 + LLM 통합은 W7+)
        List<CommunityDimension.PainPoint> painPoints = List.of();

        var result = new CommunityDimension(
                gameId,
                game.getName(),
                total,
                mentionsByPlatform,
                sentiment,
                activityZ,
                activityClass,
                painPoints,
                confidence,
                Instant.now());

        redisCache.put(cacheKey, result, CACHE_TTL);
        return result;
    }

    /** 전체 게임 mention 합계 분포 → 이 게임의 Z-score. */
    private Double computeActivityZScore(long gameId, long totalForThisGame) {
        List<Object[]> sums = mentionRepo.sumMentionsByGame();
        if (sums.size() < 2) return null; // 비교 모집단 부족

        double[] values = sums.stream()
                .mapToDouble(row -> ((Number) row[1]).doubleValue())
                .toArray();
        var stats = normalizer.normalizeWithStats(values);
        if (stats.stdDev() == 0.0) return null;

        return Math.round(normalizer.normalize(totalForThisGame, stats.mean(), stats.stdDev()) * 100.0) / 100.0;
    }

    /** Z-score → 활성도 등급. ±1σ 기준. */
    private static String classifyActivity(Double z) {
        if (z == null) return "UNKNOWN";
        if (z >= 2.0) return "VERY_ACTIVE";
        if (z >= 1.0) return "ACTIVE";
        if (z >= -1.0) return "NORMAL";
        if (z >= -2.0) return "QUIET";
        return "VERY_QUIET";
    }

    /**
     * 신뢰도 — mention 양 + sentiment 분류 비율.
     *
     * <p>HIGH: total ≥ 5000 + sentiment 분류 ≥ 80% / MEDIUM: total ≥ 1000 / LOW: 그 외.
     */
    private static Confidence assessConfidence(long total, long classified) {
        if (total == 0) return Confidence.LOW;
        double classifiedRatio = (double) classified / total;
        if (total >= 5000 && classifiedRatio >= 0.8) return Confidence.HIGH;
        if (total >= 1000) return Confidence.MEDIUM;
        return Confidence.LOW;
    }
}
