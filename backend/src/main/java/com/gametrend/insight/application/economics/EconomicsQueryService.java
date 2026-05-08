package com.gametrend.insight.application.economics;

import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PriceSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PriceSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P2 매출 + 단가 탭 데이터 조회. 5개 리포지토리에서 부분 조립 후 계산기에 전달.
 *
 * <p>입력 데이터 부재 시 nullable 필드로 graceful degradation.
 */
@Service
public class EconomicsQueryService {

    private static final Logger log = LoggerFactory.getLogger(EconomicsQueryService.class);

    /** Peak CCU 탐색 윈도우 (DAU/MAU 추정 입력). */
    public static final Duration PEAK_CCU_WINDOW = Duration.ofDays(30);

    /** Confidence 'fresh' 판정 기준 (24h). */
    public static final Duration FRESH_THRESHOLD = Duration.ofHours(24);

    /** Redis hot cache TTL — economics 데이터는 가공 결과 (룰 35: 1h 권장). W3 D3 도입. */
    public static final Duration CACHE_TTL = Duration.ofHours(1);

    private static final String CACHE_PREFIX = "economics:game:";

    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository playerRepo;
    private final PriceSnapshotJpaRepository priceRepo;
    private final ViewerSnapshotJpaRepository viewerRepo;
    private final MentionSnapshotJpaRepository mentionRepo;
    private final RevenueEstimator revenueEstimator;
    private final UnitEconomicsCalculator unitEconomicsCalculator;
    private final RedisCacheTemplate redisCache;

    public EconomicsQueryService(
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository playerRepo,
            PriceSnapshotJpaRepository priceRepo,
            ViewerSnapshotJpaRepository viewerRepo,
            MentionSnapshotJpaRepository mentionRepo,
            RevenueEstimator revenueEstimator,
            UnitEconomicsCalculator unitEconomicsCalculator,
            RedisCacheTemplate redisCache) {
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
        this.priceRepo = priceRepo;
        this.viewerRepo = viewerRepo;
        this.mentionRepo = mentionRepo;
        this.revenueEstimator = revenueEstimator;
        this.unitEconomicsCalculator = unitEconomicsCalculator;
        this.redisCache = redisCache;
    }

    @Transactional(readOnly = true)
    public EconomicsInsight getEconomicsInsight(long gameId) {
        if (!gameRepo.existsById(gameId)) {
            throw new GameNotFoundException(gameId);
        }

        // L1: Redis hot cache (existsById 통과 후 → 캐시 hit이면 빠른 반환)
        String cacheKey = CACHE_PREFIX + gameId;
        Optional<EconomicsInsight> hot = redisCache.get(cacheKey, EconomicsInsight.class);
        if (hot.isPresent()) {
            log.debug("Economics L1 Redis HIT — gameId={}", gameId);
            return hot.get();
        }

        Instant now = Instant.now();

        // 1. SteamSpy owners
        Optional<PlayerSnapshotJpaEntity> ownersSnap = playerRepo.findLatestWithOwners(gameId);
        Long ownersLow = ownersSnap.map(PlayerSnapshotJpaEntity::getOwnersLow).orElse(null);
        Long ownersHigh = ownersSnap.map(PlayerSnapshotJpaEntity::getOwnersHigh).orElse(null);

        // 2. 최신 가격
        Optional<PriceSnapshotJpaEntity> priceSnap = priceRepo.findFirstByGameIdOrderByCapturedAtDesc(gameId);
        Long priceCents = priceSnap.map(PriceSnapshotJpaEntity::getPriceCents).orElse(null);

        // 3. CCU peak (지난 30일)
        Integer ccuPeak = playerRepo.findPeakCcuSince(gameId, now.minus(PEAK_CCU_WINDOW));

        // 4. 최신 일반 PlayerSnapshot (CCU + 리뷰) - 단가 계산용
        Optional<PlayerSnapshotJpaEntity> latestPlayer = playerRepo
                .findByGameIdOrderByCapturedAtDesc(gameId, PageRequest.of(0, 5))
                .stream()
                .filter(s -> s.getConcurrentPlayers() != null)
                .findFirst();
        Integer latestCcu = latestPlayer.map(PlayerSnapshotJpaEntity::getConcurrentPlayers).orElse(null);
        Integer reviewPositive = latestPlayer.map(PlayerSnapshotJpaEntity::getReviewScorePositive).orElse(null);

        // 5. 최신 Twitch 시청자
        Optional<ViewerSnapshotJpaEntity> latestViewer = viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(gameId);
        Integer twitchViewers = latestViewer.map(ViewerSnapshotJpaEntity::getConcurrentViewers).orElse(null);

        // 6. YT+Reddit mentions 합산
        Integer totalMentions = sumMentions(gameId);

        // 계산
        EconomicsInsight.RevenueEstimate revenue =
                revenueEstimator.estimate(ownersLow, ownersHigh, priceCents, ccuPeak);
        EconomicsInsight.UnitEconomics unit = unitEconomicsCalculator.calculate(
                latestCcu, twitchViewers, totalMentions, priceCents, reviewPositive);

        boolean fresh = ownersSnap
                .map(s -> s.getCapturedAt() != null
                        && s.getCapturedAt().isAfter(now.minus(FRESH_THRESHOLD)))
                .orElse(false);
        Confidence confidence = revenueEstimator.assessConfidence(ownersLow, ownersHigh, priceCents, fresh);

        Instant lastUpdated = latestSnapshotTime(ownersSnap, priceSnap, latestPlayer, latestViewer);

        EconomicsInsight result = new EconomicsInsight(gameId, revenue, unit, confidence, lastUpdated);
        redisCache.put(cacheKey, result, CACHE_TTL);
        return result;
    }

    private Integer sumMentions(long gameId) {
        Integer total = null;
        for (SnapshotSource src : List.of(SnapshotSource.YOUTUBE, SnapshotSource.REDDIT)) {
            Optional<MentionSnapshotJpaEntity> m =
                    mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(gameId, src);
            if (m.isPresent()) {
                int count = m.get().getMentionCount();
                total = (total == null) ? count : total + count;
            }
        }
        return total;
    }

    private Instant latestSnapshotTime(
            Optional<PlayerSnapshotJpaEntity> owners,
            Optional<PriceSnapshotJpaEntity> price,
            Optional<PlayerSnapshotJpaEntity> player,
            Optional<ViewerSnapshotJpaEntity> viewer) {
        Instant latest = null;
        latest = max(latest, owners.map(PlayerSnapshotJpaEntity::getCapturedAt).orElse(null));
        latest = max(latest, price.map(PriceSnapshotJpaEntity::getCapturedAt).orElse(null));
        latest = max(latest, player.map(PlayerSnapshotJpaEntity::getCapturedAt).orElse(null));
        latest = max(latest, viewer.map(ViewerSnapshotJpaEntity::getCapturedAt).orElse(null));
        return latest;
    }

    private static Instant max(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
