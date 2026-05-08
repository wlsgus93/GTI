package com.gametrend.insight.application.ingestion;

import com.gametrend.insight.application.port.out.AppleChartsPort;
import com.gametrend.insight.application.port.out.AppleChartsPort.TopAppEntry;
import com.gametrend.insight.application.port.out.SteamSpyPort.SteamSpyEstimates;
import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import com.gametrend.insight.domain.snapshot.ViewerSnapshot;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PriceSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PriceSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일일 수집 스케줄러. 매일 03:00 UTC에 모든 게임에 대해 9소스 동시 수집.
 *
 * <p>책임:
 * <ul>
 *   <li>Game 순회 + IngestionTarget 조립
 *   <li>Orchestrator 호출 (Virtual Threads 병렬, 부분 장애 격리)
 *   <li>{@link IngestionResult.Success} 결과를 도메인 타입별로 dispatch → JPA save
 * </ul>
 *
 * <p>persistence는 per-game 트랜잭션 (한 게임 실패가 다른 게임 차단 X — 부분 장애 격리).
 *
 * <p>수동 트리거: {@code POST /api/v1/admin/ingestion/run} → {@link #runOnce()}.
 */
@Service
public class DailyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DailyIngestionService.class);

    private final IngestionOrchestrator orchestrator;
    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository playerSnapshotRepo;
    private final PriceSnapshotJpaRepository priceSnapshotRepo;
    private final ViewerSnapshotJpaRepository viewerSnapshotRepo;
    private final MentionSnapshotJpaRepository mentionSnapshotRepo;
    private final AppleChartsPort appleCharts;

    public DailyIngestionService(
            IngestionOrchestrator orchestrator,
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository playerSnapshotRepo,
            PriceSnapshotJpaRepository priceSnapshotRepo,
            ViewerSnapshotJpaRepository viewerSnapshotRepo,
            MentionSnapshotJpaRepository mentionSnapshotRepo,
            AppleChartsPort appleCharts) {
        this.orchestrator = orchestrator;
        this.gameRepo = gameRepo;
        this.playerSnapshotRepo = playerSnapshotRepo;
        this.priceSnapshotRepo = priceSnapshotRepo;
        this.viewerSnapshotRepo = viewerSnapshotRepo;
        this.mentionSnapshotRepo = mentionSnapshotRepo;
        this.appleCharts = appleCharts;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    public IngestionRunSummary runDaily() {
        return runOnce();
    }

    /** 수동 트리거. 모든 게임 순회 + 소스별 병렬 호출 + DB 적재. */
    public IngestionRunSummary runOnce() {
        long startedAt = System.currentTimeMillis();
        List<GameJpaEntity> games = gameRepo.findAll();
        log.info("Daily ingestion started: {} games", games.size());

        int success = 0;
        int failure = 0;
        int empty = 0;
        int persisted = 0;

        for (GameJpaEntity g : games) {
            // 매핑된 식별자 다 채움 — null은 orchestrator가 해당 어댑터 skip.
            // 현재 GameJpaEntity 컬럼: steam_app_id, igdb_id, name (3종). twitch_game_id / opencritic_id는 후속.
            // 결과적으로 6 어댑터 동작 가능 (Steam Web/Storefront/Spy + IGDB + YouTube + Reddit).
            if (g.getSteamAppId() == null && g.getIgdbId() == null && g.getName() == null) {
                continue;
            }
            IngestionTarget target = new IngestionTarget(
                    g.getId(),
                    g.getSteamAppId(),
                    g.getTwitchGameId(),
                    g.getIgdbId(),
                    g.getName(),
                    g.getOpencriticId());
            List<IngestionResult<?>> results = orchestrator.ingest(target);
            for (IngestionResult<?> r : results) {
                if (r instanceof IngestionResult.Success<?> ok) {
                    success++;
                    if (persistOne(ok.value(), g.getId())) persisted++;
                } else if (r instanceof IngestionResult.Failure<?>) {
                    failure++;
                } else if (r instanceof IngestionResult.Empty<?>) {
                    empty++;
                }
            }
        }

        long durationMs = System.currentTimeMillis() - startedAt;
        IngestionRunSummary summary = new IngestionRunSummary(
                games.size(), success, failure, empty, persisted, durationMs);
        log.info("Daily ingestion done: {}", summary);
        return summary;
    }

    /**
     * Success value를 도메인 타입별 dispatch → 해당 repository.save.
     *
     * <p>각 save는 Spring Data JPA 기본 트랜잭션 — 한 row 실패가 다른 row 차단 X.
     *
     * @return 적재 성공 시 true, 미지원 타입이면 false (skip + log)
     */
    @Transactional
    protected boolean persistOne(Object value, long gameId) {
        try {
            if (value instanceof PlayerSnapshot ps) {
                playerSnapshotRepo.save(PlayerSnapshotJpaEntity.from(ps));
                return true;
            } else if (value instanceof PriceSnapshot ps) {
                priceSnapshotRepo.save(PriceSnapshotJpaEntity.from(ps));
                return true;
            } else if (value instanceof ViewerSnapshot vs) {
                viewerSnapshotRepo.save(ViewerSnapshotJpaEntity.from(vs));
                return true;
            } else if (value instanceof MentionSnapshot ms) {
                mentionSnapshotRepo.save(MentionSnapshotJpaEntity.from(ms));
                return true;
            } else if (value instanceof SteamSpyEstimates est) {
                // SteamSpy → PlayerSnapshot (owners_low/high) 변환 후 적재.
                PlayerSnapshot ps = SteamSpyMapper.toPlayerSnapshot(est, gameId, Instant.now());
                if (ps == null) {
                    log.debug("SteamSpy estimates skipped (no owners/ccu): gameId={}", gameId);
                    return false;
                }
                playerSnapshotRepo.save(PlayerSnapshotJpaEntity.from(ps));
                return true;
            } else {
                // IGDB metadata, OpenCritic score 등 — 별도 모델 필요 (후속)
                log.debug("Ingestion result type not persisted: {}", value.getClass().getSimpleName());
                return false;
            }
        } catch (Exception ex) {
            log.warn("Persist failed: type={} error={}", value.getClass().getSimpleName(), ex.getMessage());
            return false;
        }
    }

    /**
     * Apple Top Charts 발견 — 카테고리/국가별 상위 앱 목록 조회.
     *
     * <p>per-game이 아닌 카테고리 호출이라 일반 ingestion 루프와 별개. 게임 마스터 발견(discovery) 용도.
     * 현재는 응답 검증만 — 새 게임 자동 등록은 후속 (Apple ID와 Steam appid 매핑 메커니즘 필요).
     */
    public AppleChartsSummary fetchAppleTopFree(String country, int limit) {
        long startedAt = System.currentTimeMillis();
        var entries = appleCharts.fetchTopFreeGames(country, limit).orElse(List.of());
        long durationMs = System.currentTimeMillis() - startedAt;
        log.info("Apple Top Charts: country={} limit={} returned={} {}ms",
                country, limit, entries.size(), durationMs);
        return new AppleChartsSummary(country, limit, entries.size(), durationMs, entries);
    }

    public record IngestionRunSummary(
            int totalGames,
            int sourceSuccesses,
            int sourceFailures,
            int sourceEmpty,
            int persistedRows,
            long durationMs) {}

    public record AppleChartsSummary(
            String country,
            int limitRequested,
            int returned,
            long durationMs,
            List<TopAppEntry> entries) {}
}
