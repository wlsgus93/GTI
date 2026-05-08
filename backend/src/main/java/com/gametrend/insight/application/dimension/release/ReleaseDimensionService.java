package com.gametrend.insight.application.dimension.release;

import com.gametrend.insight.application.stats.HitFlopClassifier;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.game.GenreJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D1 출시 동향 차원 v1 — 장르별 + 연도별 집계.
 *
 * <p>v1 한계 (W4+ 보강):
 * <ul>
 *   <li>N+1 query (게임마다 최신 스냅샷 조회) — 시드 10개라 OK
 *   <li>"성공률" 정의 부재 — 단순 평균 CCU. 후속: 장르 평균 대비 z-score로 hit/flop 분류
 *   <li>출시 후 첫 30일 CCU 같은 시기 보정 X (오래된 게임이라도 현재 CCU만 봄)
 * </ul>
 */
@Service
public class ReleaseDimensionService {

    public static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String CACHE_KEY = "dim:d1:release:v1";

    /** Hit/Flop 분류 임계 (장르 평균 ± 1σ). */
    public static final double HIT_FLOP_THRESHOLD = 1.0;

    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository playerRepo;
    private final RedisCacheTemplate redisCache;
    private final HitFlopClassifier hitFlopClassifier;

    public ReleaseDimensionService(
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository playerRepo,
            RedisCacheTemplate redisCache,
            HitFlopClassifier hitFlopClassifier) {
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
        this.redisCache = redisCache;
        this.hitFlopClassifier = hitFlopClassifier;
    }

    /** 게임 + 최신 CCU 묶음 — 집계 단계의 입력. */
    private record GameWithCcu(GameJpaEntity game, Integer latestCcu) {}

    @Transactional(readOnly = true)
    public ReleaseDimension getReleaseDimension() {
        Optional<ReleaseDimension> hot = redisCache.get(CACHE_KEY, ReleaseDimension.class);
        if (hot.isPresent()) return hot.get();

        List<GameJpaEntity> games = gameRepo.findAll();

        List<GameWithCcu> raw = games.stream()
                .map(g -> new GameWithCcu(g, latestCcu(g.getId())))
                .toList();

        ReleaseDimension result = new ReleaseDimension(
                aggregateByGenre(raw),
                aggregateByYear(raw),
                raw.size(),
                Instant.now());
        redisCache.put(CACHE_KEY, result, CACHE_TTL); // 빈 결과도 캐시 (DB 반복 조회 방지)
        return result;
    }

    private Integer latestCcu(long gameId) {
        return playerRepo.findByGameIdOrderByCapturedAtDesc(gameId, PageRequest.of(0, 1))
                .stream()
                .map(PlayerSnapshotJpaEntity::getConcurrentPlayers)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<ReleaseDimension.GenreStats> aggregateByGenre(List<GameWithCcu> raw) {
        // 장르명 -> 게임명/ccu 페어 누적
        Map<String, List<GameWithCcu>> byGenre = new HashMap<>();
        for (GameWithCcu r : raw) {
            for (GenreJpaEntity gn : r.game().getGenres()) {
                byGenre.computeIfAbsent(gn.getName(), k -> new ArrayList<>()).add(r);
            }
        }

        return byGenre.entrySet().stream()
                .map(e -> {
                    List<GameWithCcu> bucket = e.getValue();
                    List<Integer> ccus = bucket.stream()
                            .map(GameWithCcu::latestCcu)
                            .filter(Objects::nonNull)
                            .toList();
                    Integer avgCcu = ccus.isEmpty()
                            ? null
                            : (int) ccus.stream().mapToInt(Integer::intValue).average().orElse(0);
                    Integer maxCcu = ccus.stream().max(Integer::compareTo).orElse(null);
                    String topGame = bucket.stream()
                            .filter(b -> b.latestCcu() != null)
                            .max(Comparator.comparingInt(GameWithCcu::latestCcu))
                            .map(b -> b.game().getName())
                            .orElseGet(() -> bucket.isEmpty() ? null : bucket.get(0).game().getName());

                    // W5 D1 — Hit/Flop 장르 내 분류 (Z-score 기반)
                    var hitFlopCounts = classifyHitFlop(bucket);

                    return new ReleaseDimension.GenreStats(
                            e.getKey(), bucket.size(), avgCcu, maxCcu, topGame,
                            hitFlopCounts.get(HitFlopClassifier.Classification.HIT),
                            hitFlopCounts.get(HitFlopClassifier.Classification.FLOP),
                            hitFlopCounts.get(HitFlopClassifier.Classification.NORMAL));
                })
                .sorted(Comparator.comparingInt(ReleaseDimension.GenreStats::gameCount).reversed())
                .toList();
    }

    /** 장르 내 게임들의 CCU를 Z-score로 정규화 후 HIT/NORMAL/FLOP 카운트. */
    private java.util.Map<HitFlopClassifier.Classification, Long> classifyHitFlop(List<GameWithCcu> bucket) {
        java.util.Map<String, Double> ccuMap = new java.util.LinkedHashMap<>();
        for (GameWithCcu r : bucket) {
            if (r.latestCcu() != null) {
                ccuMap.put(r.game().getName(), r.latestCcu().doubleValue());
            }
        }
        if (ccuMap.isEmpty()) {
            // 모든 게임 CCU 없음 — 전부 NORMAL로 간주
            java.util.Map<HitFlopClassifier.Classification, Long> empty = new java.util.LinkedHashMap<>();
            empty.put(HitFlopClassifier.Classification.HIT, 0L);
            empty.put(HitFlopClassifier.Classification.NORMAL, (long) bucket.size());
            empty.put(HitFlopClassifier.Classification.FLOP, 0L);
            return empty;
        }
        var classified = hitFlopClassifier.classifyAll(ccuMap, HIT_FLOP_THRESHOLD);
        return hitFlopClassifier.countByClassification(classified);
    }

    private List<ReleaseDimension.YearStats> aggregateByYear(List<GameWithCcu> raw) {
        Map<Integer, List<Integer>> byYear = new HashMap<>();
        for (GameWithCcu r : raw) {
            if (r.game().getReleaseDate() == null) continue;
            int year = r.game().getReleaseDate().getYear();
            byYear.computeIfAbsent(year, k -> new ArrayList<>()).add(r.latestCcu()); // null 포함 가능
        }

        return byYear.entrySet().stream()
                .map(e -> {
                    List<Integer> all = e.getValue();
                    List<Integer> nonNull = all.stream().filter(Objects::nonNull).toList();
                    Integer avg = nonNull.isEmpty()
                            ? null
                            : (int) nonNull.stream().mapToInt(Integer::intValue).average().orElse(0);
                    return new ReleaseDimension.YearStats(e.getKey(), all.size(), avg);
                })
                .sorted(Comparator.comparingInt(ReleaseDimension.YearStats::year).reversed())
                .toList();
    }
}
