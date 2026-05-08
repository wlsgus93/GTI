package com.gametrend.insight.application.ingestion;

import com.gametrend.insight.application.port.out.IgdbPort;
import com.gametrend.insight.application.port.out.OpenCriticPort;
import com.gametrend.insight.application.port.out.RedditPort;
import com.gametrend.insight.application.port.out.SteamSpyPort;
import com.gametrend.insight.application.port.out.SteamStorefrontPort;
import com.gametrend.insight.application.port.out.SteamWebPort;
import com.gametrend.insight.application.port.out.TwitchPort;
import com.gametrend.insight.application.port.out.YouTubePort;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 9개 외부 소스 동시 수집 오케스트레이터.
 *
 * <p>핵심 설계:
 * <ul>
 *   <li>Virtual Thread Executor로 모든 소스 동시 호출 — 9× per-source latency 대신 max(latency) 달성
 *   <li>{@link IngestionResult} sealed 타입으로 부분 장애 표현
 *   <li>한 소스 실패가 다른 소스를 절대 차단하지 않음 (try/catch per source)
 *   <li>각 소스 wall-clock latency 기록 → 메트릭 + 포트폴리오 케이스 데이터
 *   <li>Day 6: 회로 차단기 적용 (어댑터 베이스에서) — 지속 장애 소스 자동 격리
 * </ul>
 *
 * <p>Day 6 스코프: per-game 8 어댑터 (Steam Web, Storefront, SteamSpy, Twitch, IGDB, YouTube, Reddit, OpenCritic).
 * Apple RSS는 카테고리 일괄 호출이라 별도 플로우.
 */
@Component
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);

    private final SteamWebPort steamWeb;
    private final SteamStorefrontPort steamStorefront;
    private final TwitchPort twitch;
    private final IgdbPort igdb;
    private final YouTubePort youtube;
    private final RedditPort reddit;
    private final OpenCriticPort openCritic;
    private final SteamSpyPort steamSpy;
    private final ExecutorService executor;

    public IngestionOrchestrator(
            SteamWebPort steamWeb,
            SteamStorefrontPort steamStorefront,
            TwitchPort twitch,
            IgdbPort igdb,
            YouTubePort youtube,
            RedditPort reddit,
            OpenCriticPort openCritic,
            SteamSpyPort steamSpy,
            @Qualifier("virtualThreadExecutor") ExecutorService executor) {
        this.steamWeb = steamWeb;
        this.steamStorefront = steamStorefront;
        this.twitch = twitch;
        this.igdb = igdb;
        this.youtube = youtube;
        this.reddit = reddit;
        this.openCritic = openCritic;
        this.steamSpy = steamSpy;
        this.executor = executor;
    }

    public List<IngestionResult<?>> ingest(IngestionTarget target) {
        Instant overallStart = Instant.now();
        List<CompletableFuture<IngestionResult<?>>> futures = new ArrayList<>();

        if (target.steamAppId() != null) {
            futures.add(submit(SnapshotSource.STEAM, () -> steamWeb.fetchCurrentPlayers(target.gameId(), target.steamAppId())));
            futures.add(submit(SnapshotSource.STEAM_STORE, () -> steamStorefront.fetchPrice(target.gameId(), target.steamAppId())));
            futures.add(submit(SnapshotSource.STEAM_SPY, () -> steamSpy.fetchEstimates(target.gameId(), target.steamAppId())));
        }
        if (target.twitchGameId() != null) {
            futures.add(submit(SnapshotSource.TWITCH, () -> twitch.fetchViewers(target.gameId(), target.twitchGameId())));
        }
        if (target.igdbId() != null) {
            futures.add(submit(SnapshotSource.IGDB, () -> igdb.fetchGameMetadata(target.igdbId())));
        }
        if (target.gameName() != null) {
            futures.add(submit(SnapshotSource.YOUTUBE, () -> youtube.fetchMentionCount(target.gameId(), target.gameName())));
            futures.add(submit(SnapshotSource.REDDIT, () -> reddit.fetchMentionCount(target.gameId(), target.gameName())));
        }
        if (target.openCriticGameId() != null) {
            futures.add(submit(SnapshotSource.OPENCRITIC, () -> openCritic.fetchScore(target.gameId(), target.openCriticGameId())));
        }
        // 주: Apple RSS는 per-game 호출이 아님 — 카테고리/국가별 일괄 조회용 별도 플로우

        List<IngestionResult<?>> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        Duration overall = Duration.between(overallStart, Instant.now());
        log.info(
                "Ingestion done: gameId={} sources={} wall-clock={}ms successes={} failures={}",
                target.gameId(),
                results.size(),
                overall.toMillis(),
                results.stream().filter(r -> r instanceof IngestionResult.Success<?>).count(),
                results.stream().filter(r -> r instanceof IngestionResult.Failure<?>).count());

        return results;
    }

    private <T> CompletableFuture<IngestionResult<?>> submit(SnapshotSource source, Supplier<Optional<T>> task) {
        return CompletableFuture.supplyAsync(() -> runOne(source, task), executor);
    }

    private <T> IngestionResult<?> runOne(SnapshotSource source, Supplier<Optional<T>> task) {
        Instant start = Instant.now();
        try {
            Optional<T> value = task.get();
            Duration d = Duration.between(start, Instant.now());
            return value.<IngestionResult<?>>map(v -> IngestionResult.success(source, v, d))
                    .orElseGet(() -> IngestionResult.empty(source, d));
        } catch (Throwable t) {
            Duration d = Duration.between(start, Instant.now());
            log.warn("Ingestion failed: source={} duration={}ms error={}", source, d.toMillis(), t.getMessage());
            return IngestionResult.failure(source, t, d);
        }
    }
}
