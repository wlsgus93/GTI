package com.gametrend.insight.application.game;

import com.gametrend.insight.application.trend.TrendScoreCalculator;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.game.GenreJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P2 게임 상세 데이터 조회 서비스.
 *
 * <p>책임:
 * <ul>
 *   <li>{@link #getDetail(long)} — 게임 메타 + 최신 CCU/변화율 (개요 탭)
 *   <li>{@link #getCcuSeries(long, CcuRange)} — 동접 시계열 (CCU 탭)
 *   <li>{@link #getPlayerInsight(long)} — 최신 CCU/리뷰/시청자/멘션 (플레이어 분석 탭, W2 Day 3)
 * </ul>
 *
 * <p>예외 처리: 게임 미존재 → {@link GameNotFoundException}.
 */
@Service
public class GameQueryService {

    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository snapshotRepo;
    private final ViewerSnapshotJpaRepository viewerRepo;
    private final MentionSnapshotJpaRepository mentionRepo;
    private final TrendScoreCalculator scoreCalculator;

    public GameQueryService(
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository snapshotRepo,
            ViewerSnapshotJpaRepository viewerRepo,
            MentionSnapshotJpaRepository mentionRepo,
            TrendScoreCalculator scoreCalculator) {
        this.gameRepo = gameRepo;
        this.snapshotRepo = snapshotRepo;
        this.viewerRepo = viewerRepo;
        this.mentionRepo = mentionRepo;
        this.scoreCalculator = scoreCalculator;
    }

    @Transactional(readOnly = true)
    public GameDetailItem getDetail(long gameId) {
        GameJpaEntity game = gameRepo.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));

        List<PlayerSnapshotJpaEntity> recent =
                snapshotRepo.findByGameIdOrderByCapturedAtDesc(gameId, PageRequest.of(0, 2));
        Integer latestCcu = recent.isEmpty() ? null : recent.get(0).getConcurrentPlayers();
        Double deltaPct = (recent.size() >= 2 && latestCcu != null && recent.get(1).getConcurrentPlayers() != null)
                ? scoreCalculator.calculateDeltaPct(latestCcu.longValue(), recent.get(1).getConcurrentPlayers().longValue())
                : null;

        return new GameDetailItem(
                game.getId(),
                game.getSteamAppId(),
                game.getIgdbId(),
                game.getName(),
                game.getDescription(),
                game.getDeveloper(),
                game.getPublisher(),
                game.getReleaseDate(),
                game.getCoverImageUrl(),
                game.getGenres().stream().map(GenreJpaEntity::getName).toList(),
                latestCcu,
                deltaPct,
                game.getCreatedAt(),
                game.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public CcuSeries getCcuSeries(long gameId, CcuRange range) {
        if (!gameRepo.existsById(gameId)) {
            throw new GameNotFoundException(gameId);
        }

        Instant to = Instant.now();
        Instant from = to.minus(range.duration());

        List<PlayerSnapshotJpaEntity> snapshots =
                snapshotRepo.findByGameIdAndCapturedAtAfterOrderByCapturedAtAsc(gameId, from);

        List<CcuSeries.Point> points = snapshots.stream()
                .filter(s -> s.getConcurrentPlayers() != null)
                .map(s -> new CcuSeries.Point(s.getCapturedAt(), s.getConcurrentPlayers()))
                .toList();

        return new CcuSeries(gameId, range.code(), from, to, points);
    }

    /**
     * P2 플레이어 분석 탭. 각 데이터는 독립적으로 nullable (소스별 ingestion 진행도에 따라).
     */
    @Transactional(readOnly = true)
    public PlayerInsight getPlayerInsight(long gameId) {
        if (!gameRepo.existsById(gameId)) {
            throw new GameNotFoundException(gameId);
        }

        // 1. 최신 PlayerSnapshot
        Optional<PlayerSnapshotJpaEntity> latestPlayer = snapshotRepo
                .findByGameIdOrderByCapturedAtDesc(gameId, PageRequest.of(0, 1))
                .stream()
                .findFirst();

        PlayerInsight.PlayerStats stats = latestPlayer
                .map(p -> {
                    Double percent = (p.getReviewScoreTotal() != null
                                    && p.getReviewScoreTotal() > 0
                                    && p.getReviewScorePositive() != null)
                            ? round1((double) p.getReviewScorePositive() / p.getReviewScoreTotal() * 100.0)
                            : null;
                    return new PlayerInsight.PlayerStats(
                            p.getConcurrentPlayers(),
                            p.getReviewScorePositive(),
                            p.getReviewScoreTotal(),
                            percent);
                })
                .orElse(new PlayerInsight.PlayerStats(null, null, null, null));

        // 2. 최신 ViewerSnapshot (Twitch)
        Optional<ViewerSnapshotJpaEntity> latestViewer = viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(gameId);
        Integer twitchViewers = latestViewer.map(ViewerSnapshotJpaEntity::getConcurrentViewers).orElse(null);

        // 3. 플랫폼별 최신 MentionSnapshot (YouTube + Reddit)
        List<PlayerInsight.MentionByPlatform> mentions = new ArrayList<>();
        for (SnapshotSource src : List.of(SnapshotSource.YOUTUBE, SnapshotSource.REDDIT)) {
            mentionRepo
                    .findFirstByGameIdAndSourceOrderByCapturedAtDesc(gameId, src)
                    .ifPresent(m -> mentions.add(new PlayerInsight.MentionByPlatform(
                            src.name(), m.getMentionCount(), m.getCapturedAt())));
        }

        Instant lastUpdated = latestSnapshotTime(latestPlayer, latestViewer, mentions);

        return new PlayerInsight(gameId, stats, twitchViewers, mentions, lastUpdated);
    }

    private Instant latestSnapshotTime(
            Optional<PlayerSnapshotJpaEntity> p,
            Optional<ViewerSnapshotJpaEntity> v,
            List<PlayerInsight.MentionByPlatform> mentions) {
        Instant latest = null;
        if (p.isPresent() && p.get().getCapturedAt() != null) {
            latest = p.get().getCapturedAt();
        }
        if (v.isPresent()
                && v.get().getCapturedAt() != null
                && (latest == null || v.get().getCapturedAt().isAfter(latest))) {
            latest = v.get().getCapturedAt();
        }
        for (var m : mentions) {
            if (m.capturedAt() != null && (latest == null || m.capturedAt().isAfter(latest))) {
                latest = m.capturedAt();
            }
        }
        return latest;
    }

    private double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
