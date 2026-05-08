package com.gametrend.insight.application.trend;

import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P1 트렌드 보드 데이터 조회 서비스.
 *
 * <p>Day 1 v1 흐름:
 * <ol>
 *   <li>모든 게임 조회 (Top 50 cap)
 *   <li>각 게임의 최근 스냅샷 1~2건 조회 (현재 CCU + 24h 전 CCU)
 *   <li>{@link TrendScoreCalculator}로 score + deltaPct 계산
 *   <li>score desc 정렬 + limit
 * </ol>
 *
 * <p>v1 한계:
 * <ul>
 *   <li>N+1 쿼리 (게임마다 스냅샷 query) — 게임 10개 시드라 OK. W2 Day 2에 batch query 최적화
 *   <li>장르는 placeholder. D4 장르 클러스터링은 W3
 *   <li>플랫폼은 "Steam" 고정. 모바일 등은 W3+
 * </ul>
 */
@Service
public class TrendQueryService {

    private static final Logger log = LoggerFactory.getLogger(TrendQueryService.class);

    private static final String DEFAULT_GENRE = "Game";
    private static final String DEFAULT_PLATFORM = "Steam";

    /** Trend ranking은 자주 변하므로 짧은 TTL (룰 35: 메타 24h, 동적 5m). W3 D3 도입. */
    public static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private static final String CACHE_PREFIX = "trends:limit:";

    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository snapshotRepo;
    private final TrendScoreCalculator calculator;
    private final RedisCacheTemplate redisCache;

    public TrendQueryService(
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository snapshotRepo,
            TrendScoreCalculator calculator,
            RedisCacheTemplate redisCache) {
        this.gameRepo = gameRepo;
        this.snapshotRepo = snapshotRepo;
        this.calculator = calculator;
        this.redisCache = redisCache;
    }

    /**
     * Cache 직렬화용 wrapper — Jackson이 List 자체를 일반 직렬화 못 함 (Class&lt;T&gt; 시그니처).
     * record 안에 감싸서 RedisCacheTemplate.get/put에 사용.
     */
    public record CachedTrendBoard(List<TrendBoardItem> items) {}

    @Transactional(readOnly = true)
    public List<TrendBoardItem> topByTrendScore(int limit) {
        // L1: Redis hot cache
        String cacheKey = CACHE_PREFIX + limit;
        Optional<CachedTrendBoard> hot = redisCache.get(cacheKey, CachedTrendBoard.class);
        if (hot.isPresent()) {
            log.debug("Trend L1 Redis HIT — limit={}", limit);
            return hot.get().items();
        }

        List<GameJpaEntity> games = gameRepo.findAll();
        if (games.isEmpty()) {
            return List.of();
        }

        // 1. 각 게임의 최신 2건 스냅샷 (현재 + 직전) 조회
        record GameWithSnapshots(GameJpaEntity game, Integer current, Integer previous) {}
        List<GameWithSnapshots> raw = new ArrayList<>(games.size());
        long maxCcu = 1L;

        for (GameJpaEntity g : games) {
            List<PlayerSnapshotJpaEntity> recent =
                    snapshotRepo.findByGameIdOrderByCapturedAtDesc(g.getId(), PageRequest.of(0, 2));
            Integer current = recent.isEmpty() ? null : recent.get(0).getConcurrentPlayers();
            Integer previous = recent.size() >= 2 ? recent.get(1).getConcurrentPlayers() : null;
            raw.add(new GameWithSnapshots(g, current, previous));
            if (current != null && current > maxCcu) {
                maxCcu = current;
            }
        }

        // 2. score 계산 + deltaPct + 정렬
        final long maxCcuFinal = maxCcu;
        List<TrendBoardItem> result = raw.stream()
                .map(r -> toBoardItem(r.game(), r.current(), r.previous(), maxCcuFinal))
                .sorted(Comparator.comparingDouble(TrendBoardItem::trendScore).reversed())
                .limit(limit)
                .toList();

        redisCache.put(cacheKey, new CachedTrendBoard(result), CACHE_TTL);
        return result;
    }

    private TrendBoardItem toBoardItem(GameJpaEntity g, Integer current, Integer previous, long maxCcu) {
        long currentCcu = current == null ? 0L : current.longValue();
        double score = calculator.calculate(currentCcu, maxCcu);
        Double deltaPct = (current != null && previous != null)
                ? calculator.calculateDeltaPct(current.longValue(), previous.longValue())
                : null;
        return new TrendBoardItem(
                String.valueOf(g.getSteamAppId() != null ? g.getSteamAppId() : g.getId()),
                g.getName(),
                DEFAULT_GENRE,
                DEFAULT_PLATFORM,
                round1(score),
                deltaPct == null ? null : round1(deltaPct),
                current == null ? 0 : current);
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
