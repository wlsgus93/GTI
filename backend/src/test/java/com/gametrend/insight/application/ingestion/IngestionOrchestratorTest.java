package com.gametrend.insight.application.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.port.out.IgdbPort;
import com.gametrend.insight.application.port.out.OpenCriticPort;
import com.gametrend.insight.application.port.out.RedditPort;
import com.gametrend.insight.application.port.out.SteamSpyPort;
import com.gametrend.insight.application.port.out.SteamStorefrontPort;
import com.gametrend.insight.application.port.out.SteamWebPort;
import com.gametrend.insight.application.port.out.TwitchPort;
import com.gametrend.insight.application.port.out.YouTubePort;
import com.gametrend.insight.domain.snapshot.MentionSnapshot;
import com.gametrend.insight.domain.snapshot.PlayerSnapshot;
import com.gametrend.insight.domain.snapshot.PriceSnapshot;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.domain.snapshot.ViewerSnapshot;
import com.gametrend.insight.infrastructure.external.common.ExternalApiException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * IngestionOrchestrator 테스트 — Virtual Threads 동시성 + 부분 장애 + 결과 매핑 검증.
 */
@ExtendWith(MockitoExtension.class)
class IngestionOrchestratorTest {

    @Mock
    private SteamWebPort steamWeb;

    @Mock
    private SteamStorefrontPort steamStorefront;

    @Mock
    private TwitchPort twitch;

    @Mock
    private IgdbPort igdb;

    @Mock
    private YouTubePort youtube;

    @Mock
    private RedditPort reddit;

    @Mock
    private OpenCriticPort openCritic;

    @Mock
    private SteamSpyPort steamSpy;

    private ExecutorService executor;
    private IngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
        orchestrator = new IngestionOrchestrator(
                steamWeb, steamStorefront, twitch, igdb, youtube, reddit, openCritic, steamSpy, executor);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("steam+twitch 모두 성공 → 3 Success + 1 Empty (SteamSpy unmocked)")
    void allSuccess() {
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong()))
                .thenReturn(Optional.of(samplePlayer(1L)));
        when(steamStorefront.fetchPrice(anyLong(), anyLong()))
                .thenReturn(Optional.of(samplePrice(1L)));
        when(twitch.fetchViewers(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleViewer(1L)));
        // SteamSpy unmocked → Mockito 기본값 Optional.empty() → Empty 결과

        var results = orchestrator.ingest(new IngestionTarget(1L, 730L, "32982"));

        // Day 6: steamAppId 있으면 Steam Web + Storefront + SteamSpy 3개 호출 + Twitch 1개 = 4
        assertThat(results).hasSize(4);
        long successCount = results.stream().filter(r -> r instanceof IngestionResult.Success<?>).count();
        assertThat(successCount).isEqualTo(3);
        assertThat(results).extracting(IngestionResult::source)
                .containsExactlyInAnyOrder(
                        SnapshotSource.STEAM,
                        SnapshotSource.STEAM_STORE,
                        SnapshotSource.STEAM_SPY,
                        SnapshotSource.TWITCH);
    }

    @Test
    @DisplayName("부분 장애: Twitch 실패해도 Steam 2개는 성공 (장애 격리)")
    void partialFailure_isolated() {
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong()))
                .thenReturn(Optional.of(samplePlayer(1L)));
        when(steamStorefront.fetchPrice(anyLong(), anyLong()))
                .thenReturn(Optional.of(samplePrice(1L)));
        when(twitch.fetchViewers(anyLong(), anyString()))
                .thenThrow(new ExternalApiException.Server("twitch", "down", null));

        var results = orchestrator.ingest(new IngestionTarget(1L, 730L, "32982"));

        long successes = results.stream().filter(r -> r instanceof IngestionResult.Success<?>).count();
        long failures = results.stream().filter(r -> r instanceof IngestionResult.Failure<?>).count();
        assertThat(successes).isEqualTo(2);
        assertThat(failures).isEqualTo(1);

        var twitchFailure = results.stream()
                .filter(r -> r.source() == SnapshotSource.TWITCH)
                .findFirst()
                .orElseThrow();
        assertThat(twitchFailure).isInstanceOf(IngestionResult.Failure.class);
    }

    @Test
    @DisplayName("Optional.empty 반환 → Empty 결과 (Success도 Failure도 아님)")
    void emptyResponse_mappedToEmpty() {
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(steamStorefront.fetchPrice(anyLong(), anyLong()))
                .thenReturn(Optional.of(samplePrice(1L)));
        when(twitch.fetchViewers(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleViewer(1L)));

        var results = orchestrator.ingest(new IngestionTarget(1L, 730L, "32982"));

        var steamResult = results.stream()
                .filter(r -> r.source() == SnapshotSource.STEAM)
                .findFirst()
                .orElseThrow();
        assertThat(steamResult).isInstanceOf(IngestionResult.Empty.class);
    }

    @Test
    @DisplayName("steamAppId 없으면 Steam 어댑터 호출 안 함")
    void noSteamAppId_skipsSteam() {
        when(twitch.fetchViewers(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleViewer(1L)));

        var results = orchestrator.ingest(new IngestionTarget(1L, null, "32982"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).source()).isEqualTo(SnapshotSource.TWITCH);
    }

    @Test
    @DisplayName("동시성: 3 어댑터가 각 100ms 걸려도 wall-clock < 200ms (병렬 실행)")
    void parallelExecution_wallClockMeasure() {
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong())).thenAnswer(invocation -> {
            Thread.sleep(100);
            return Optional.of(samplePlayer(1L));
        });
        when(steamStorefront.fetchPrice(anyLong(), anyLong())).thenAnswer(invocation -> {
            Thread.sleep(100);
            return Optional.of(samplePrice(1L));
        });
        when(twitch.fetchViewers(anyLong(), anyString())).thenAnswer(invocation -> {
            Thread.sleep(100);
            return Optional.of(sampleViewer(1L));
        });

        Instant start = Instant.now();
        var results = orchestrator.ingest(new IngestionTarget(1L, 730L, "32982"));
        long wallClockMs = Duration.between(start, Instant.now()).toMillis();

        // 순차: ~300ms (3 sleep × 100ms), 병렬: ~100ms (Virtual Threads). 200ms 컷 = 명확히 병렬
        // Day 6: steamAppId → SteamSpy 추가 (unmocked → 즉시 Empty), 그래서 size=4
        assertThat(results).hasSize(4);
        assertThat(wallClockMs).isLessThan(200L);
    }

    private static PlayerSnapshot samplePlayer(long gameId) {
        return new PlayerSnapshot(
                null, gameId, 12345, null, null, null, null, Instant.now(), SnapshotSource.STEAM, false);
    }

    private static PriceSnapshot samplePrice(long gameId) {
        return new PriceSnapshot(null, gameId, "USD", 1999L, 0, Instant.now(), SnapshotSource.STEAM_STORE, false);
    }

    private static ViewerSnapshot sampleViewer(long gameId) {
        return new ViewerSnapshot(null, gameId, 5000, Instant.now(), SnapshotSource.TWITCH, false);
    }

    @Test
    @DisplayName("Day 6 — 8 소스 (steamAppId+twitch+igdb+gameName+openCriticId) 통합 호출")
    void allEightSources_success() {
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong())).thenReturn(Optional.of(samplePlayer(1L)));
        when(steamStorefront.fetchPrice(anyLong(), anyLong())).thenReturn(Optional.of(samplePrice(1L)));
        when(twitch.fetchViewers(anyLong(), anyString())).thenReturn(Optional.of(sampleViewer(1L)));
        when(igdb.fetchGameMetadata(anyLong()))
                .thenReturn(Optional.of(new IgdbPort.IgdbGameMeta(
                        1942L, "ELDEN RING", "summary", null, java.util.List.of("RPG"), null)));
        when(youtube.fetchMentionCount(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleMention(1L, SnapshotSource.YOUTUBE, 1000)));
        when(reddit.fetchMentionCount(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleMention(1L, SnapshotSource.REDDIT, 50)));
        when(openCritic.fetchScore(anyLong(), anyLong()))
                .thenReturn(Optional.of(new com.gametrend.insight.application.port.out.OpenCriticPort
                        .OpenCriticScore(99L, "ELDEN RING", 95.0, "Mighty", 80)));
        when(steamSpy.fetchEstimates(anyLong(), anyLong()))
                .thenReturn(Optional.of(new com.gametrend.insight.application.port.out.SteamSpyPort
                        .SteamSpyEstimates(730L, "ELDEN RING", "10M..20M", 5000, 200, 50000)));

        var target = new IngestionTarget(1L, 730L, "32982", 1942L, "ELDEN RING", 99L);
        var results = orchestrator.ingest(target);

        assertThat(results).hasSize(8);
        assertThat(results).allMatch(r -> r instanceof IngestionResult.Success<?>);
        assertThat(results).extracting(IngestionResult::source)
                .containsExactlyInAnyOrder(
                        SnapshotSource.STEAM,
                        SnapshotSource.STEAM_STORE,
                        SnapshotSource.STEAM_SPY,
                        SnapshotSource.TWITCH,
                        SnapshotSource.IGDB,
                        SnapshotSource.YOUTUBE,
                        SnapshotSource.REDDIT,
                        SnapshotSource.OPENCRITIC);
    }

    private static MentionSnapshot sampleMention(long gameId, SnapshotSource source, int count) {
        return new MentionSnapshot(null, gameId, count, null, Instant.now(), source, false);
    }

    @Test
    @DisplayName("Day 7 — 8 sources × 100ms each → wall-clock < 300ms (병렬, 순차 대비 약 6×)")
    void parallelExecution_eightSources() throws InterruptedException {
        // 8 모든 포트가 100ms sleep 후 응답
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(samplePlayer(1L));
        });
        when(steamStorefront.fetchPrice(anyLong(), anyLong())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(samplePrice(1L));
        });
        when(steamSpy.fetchEstimates(anyLong(), anyLong())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(new com.gametrend.insight.application.port.out.SteamSpyPort
                    .SteamSpyEstimates(730L, "n", "1M..2M", 100, 50, 1000));
        });
        when(twitch.fetchViewers(anyLong(), anyString())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(sampleViewer(1L));
        });
        when(igdb.fetchGameMetadata(anyLong())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(new IgdbPort.IgdbGameMeta(99L, "n", "s", null, java.util.List.of(), null));
        });
        when(youtube.fetchMentionCount(anyLong(), anyString())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(sampleMention(1L, SnapshotSource.YOUTUBE, 1));
        });
        when(reddit.fetchMentionCount(anyLong(), anyString())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(sampleMention(1L, SnapshotSource.REDDIT, 1));
        });
        when(openCritic.fetchScore(anyLong(), anyLong())).thenAnswer(inv -> {
            Thread.sleep(100);
            return Optional.of(new com.gametrend.insight.application.port.out.OpenCriticPort
                    .OpenCriticScore(1L, "n", 80.0, "Strong", 10));
        });

        var target = new IngestionTarget(1L, 730L, "32982", 1942L, "ELDEN RING", 99L);

        Instant start = Instant.now();
        var results = orchestrator.ingest(target);
        long wallClockMs = Duration.between(start, Instant.now()).toMillis();

        assertThat(results).hasSize(8);
        assertThat(results).allMatch(r -> r instanceof IngestionResult.Success<?>);
        // 순차: 8 × 100ms = 800ms. Virtual Thread 병렬: ~100ms + overhead. 300ms 컷.
        assertThat(wallClockMs).isLessThan(300L);
    }

    @Test
    @DisplayName("Day 7 — 8 sources에서 1 실패 + 1 empty + 6 success → 격리 검증")
    void partialFailure_acrossEightSources() {
        when(steamWeb.fetchCurrentPlayers(anyLong(), anyLong())).thenReturn(Optional.of(samplePlayer(1L)));
        when(steamStorefront.fetchPrice(anyLong(), anyLong())).thenReturn(Optional.of(samplePrice(1L)));
        when(steamSpy.fetchEstimates(anyLong(), anyLong())).thenReturn(Optional.empty()); // Empty
        when(twitch.fetchViewers(anyLong(), anyString())).thenReturn(Optional.of(sampleViewer(1L)));
        when(igdb.fetchGameMetadata(anyLong()))
                .thenThrow(new ExternalApiException.Server("igdb", "down", null)); // Failure
        when(youtube.fetchMentionCount(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleMention(1L, SnapshotSource.YOUTUBE, 1)));
        when(reddit.fetchMentionCount(anyLong(), anyString()))
                .thenReturn(Optional.of(sampleMention(1L, SnapshotSource.REDDIT, 1)));
        when(openCritic.fetchScore(anyLong(), anyLong()))
                .thenReturn(Optional.of(new com.gametrend.insight.application.port.out.OpenCriticPort
                        .OpenCriticScore(1L, "n", 80.0, "Strong", 10)));

        var target = new IngestionTarget(1L, 730L, "32982", 1942L, "ELDEN RING", 99L);
        var results = orchestrator.ingest(target);

        long success = results.stream().filter(r -> r instanceof IngestionResult.Success<?>).count();
        long failure = results.stream().filter(r -> r instanceof IngestionResult.Failure<?>).count();
        long empty = results.stream().filter(r -> r instanceof IngestionResult.Empty<?>).count();

        assertThat(results).hasSize(8);
        assertThat(success).isEqualTo(6);
        assertThat(failure).isEqualTo(1);
        assertThat(empty).isEqualTo(1);

        // 실패한 소스 (IGDB)와 empty 소스 (SteamSpy) 정확히 식별
        assertThat(results.stream()
                        .filter(r -> r instanceof IngestionResult.Failure<?>)
                        .findFirst()
                        .orElseThrow()
                        .source())
                .isEqualTo(SnapshotSource.IGDB);
    }
}
