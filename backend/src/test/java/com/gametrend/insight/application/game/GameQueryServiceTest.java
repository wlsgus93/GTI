package com.gametrend.insight.application.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.trend.TrendScoreCalculator;
import com.gametrend.insight.domain.snapshot.SnapshotSource;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.ViewerSnapshotJpaRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class GameQueryServiceTest {

    @Mock
    private GameJpaRepository gameRepo;

    @Mock
    private PlayerSnapshotJpaRepository snapshotRepo;

    @Mock
    private ViewerSnapshotJpaRepository viewerRepo;

    @Mock
    private MentionSnapshotJpaRepository mentionRepo;

    private GameQueryService service;

    @BeforeEach
    void setUp() {
        service = new GameQueryService(gameRepo, snapshotRepo, viewerRepo, mentionRepo, new TrendScoreCalculator());
    }

    @Test
    @DisplayName("getDetail: 메타 + 최신 CCU + 24h 변화율 매핑")
    void getDetail_returnsFullMeta() {
        GameJpaEntity game = makeGame(1L, 730L, "Counter-Strike 2", "Valve");
        when(gameRepo.findById(1L)).thenReturn(Optional.of(game));
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(1_100_000), snapshot(1_050_000))); // +4.76%

        GameDetailItem detail = service.getDetail(1L);

        assertThat(detail.id()).isEqualTo(1L);
        assertThat(detail.steamAppId()).isEqualTo(730L);
        assertThat(detail.name()).isEqualTo("Counter-Strike 2");
        assertThat(detail.developer()).isEqualTo("Valve");
        assertThat(detail.latestCcu()).isEqualTo(1_100_000);
        assertThat(detail.ccuDeltaPct()).isCloseTo(4.76, org.assertj.core.api.Assertions.within(0.05));
        assertThat(detail.genres()).isEmpty();
    }

    @Test
    @DisplayName("getDetail: 스냅샷 없으면 latestCcu/deltaPct null, 메타는 정상")
    void getDetail_noSnapshots_nullsLatest() {
        GameJpaEntity game = makeGame(2L, 99L, "Indie", "Studio");
        when(gameRepo.findById(2L)).thenReturn(Optional.of(game));
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(2L), any(Pageable.class)))
                .thenReturn(List.of());

        GameDetailItem detail = service.getDetail(2L);

        assertThat(detail.latestCcu()).isNull();
        assertThat(detail.ccuDeltaPct()).isNull();
        assertThat(detail.name()).isEqualTo("Indie");
    }

    @Test
    @DisplayName("getDetail: 스냅샷 1개만 있으면 latestCcu OK + deltaPct null")
    void getDetail_singleSnapshot_deltaNull() {
        GameJpaEntity game = makeGame(3L, 300L, "Single", "Dev");
        when(gameRepo.findById(3L)).thenReturn(Optional.of(game));
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(3L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(50_000)));

        GameDetailItem detail = service.getDetail(3L);

        assertThat(detail.latestCcu()).isEqualTo(50_000);
        assertThat(detail.ccuDeltaPct()).isNull();
    }

    @Test
    @DisplayName("getDetail: 게임 미존재 → GameNotFoundException")
    void getDetail_unknown_throws() {
        when(gameRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(999L))
                .isInstanceOf(GameNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("getCcuSeries: range=7d → 7일 전 ~ 현재 스냅샷, 시간순 + 메타 채워짐")
    void getCcuSeries_returnsTimeOrderedPoints() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        Instant t1 = Instant.now().minusSeconds(86400 * 5); // 5일 전
        Instant t2 = Instant.now().minusSeconds(86400 * 2); // 2일 전
        Instant t3 = Instant.now().minusSeconds(3600); // 1시간 전
        when(snapshotRepo.findByGameIdAndCapturedAtAfterOrderByCapturedAtAsc(eq(1L), any(Instant.class)))
                .thenReturn(List.of(snapshotAt(t1, 1_000_000), snapshotAt(t2, 1_050_000), snapshotAt(t3, 1_100_000)));

        CcuSeries series = service.getCcuSeries(1L, CcuRange.DAYS_7);

        assertThat(series.gameId()).isEqualTo(1L);
        assertThat(series.range()).isEqualTo("7d");
        assertThat(series.points()).hasSize(3);
        assertThat(series.points().get(0).capturedAt()).isEqualTo(t1);
        assertThat(series.points().get(0).concurrentPlayers()).isEqualTo(1_000_000);
        assertThat(series.points().get(2).concurrentPlayers()).isEqualTo(1_100_000);
    }

    @Test
    @DisplayName("getCcuSeries: 게임 미존재 → GameNotFoundException")
    void getCcuSeries_unknown_throws() {
        when(gameRepo.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getCcuSeries(999L, CcuRange.DAYS_30))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("getCcuSeries: concurrentPlayers null인 스냅샷은 필터링됨")
    void getCcuSeries_filtersNullCcu() {
        when(gameRepo.existsById(1L)).thenReturn(true);
        Instant t = Instant.now().minusSeconds(3600);
        when(snapshotRepo.findByGameIdAndCapturedAtAfterOrderByCapturedAtAsc(eq(1L), any(Instant.class)))
                .thenReturn(List.of(snapshotAt(t, null), snapshotAt(t.plusSeconds(60), 100_000)));

        CcuSeries series = service.getCcuSeries(1L, CcuRange.DAYS_30);

        assertThat(series.points()).hasSize(1);
        assertThat(series.points().get(0).concurrentPlayers()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("CcuRange.parse: null/blank → 30d 기본")
    void ccuRange_default() {
        assertThat(CcuRange.parse(null)).isEqualTo(CcuRange.DAYS_30);
        assertThat(CcuRange.parse("")).isEqualTo(CcuRange.DAYS_30);
        assertThat(CcuRange.parse("  ")).isEqualTo(CcuRange.DAYS_30);
    }

    @Test
    @DisplayName("CcuRange.parse: 알 수 없는 코드 → IllegalArgumentException")
    void ccuRange_invalid_throws() {
        assertThatThrownBy(() -> CcuRange.parse("365d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported range");
    }

    private static GameJpaEntity makeGame(long id, long steamAppId, String name, String dev) {
        try {
            var ctor = GameJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            GameJpaEntity g = ctor.newInstance();
            Field f = GameJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(g, id);
            g.setSteamAppId(steamAppId);
            g.setName(name);
            g.setDeveloper(dev);
            g.setReleaseDate(LocalDate.of(2023, 1, 1));
            g.setGenres(new HashSet<>(Set.of()));
            return g;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayerSnapshotJpaEntity snapshot(int ccu) {
        return snapshotAt(Instant.now(), ccu);
    }

    private static PlayerSnapshotJpaEntity snapshotAt(Instant at, Integer ccu) {
        try {
            var ctor = PlayerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            PlayerSnapshotJpaEntity s = ctor.newInstance();
            s.setConcurrentPlayers(ccu);
            s.setCapturedAt(at);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayerSnapshotJpaEntity snapshotWithReviews(int ccu, int positive, int total, Instant at) {
        try {
            var ctor = PlayerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            PlayerSnapshotJpaEntity s = ctor.newInstance();
            s.setConcurrentPlayers(ccu);
            s.setReviewScorePositive(positive);
            s.setReviewScoreTotal(total);
            s.setCapturedAt(at);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ViewerSnapshotJpaEntity viewerSnapshot(int viewers, Instant at) {
        try {
            var ctor = ViewerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            ViewerSnapshotJpaEntity s = ctor.newInstance();
            s.setConcurrentViewers(viewers);
            s.setCapturedAt(at);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static MentionSnapshotJpaEntity mentionSnapshot(int count, SnapshotSource source, Instant at) {
        try {
            var ctor = MentionSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            MentionSnapshotJpaEntity s = ctor.newInstance();
            s.setMentionCount(count);
            s.setSource(source);
            s.setCapturedAt(at);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ===== Day 3: getPlayerInsight =====

    @org.junit.jupiter.api.Nested
    class PlayerInsightTests {

        @Test
        @DisplayName("게임 미존재 → GameNotFoundException")
        void unknown_throws() {
            when(gameRepo.existsById(999L)).thenReturn(false);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getPlayerInsight(999L))
                    .isInstanceOf(GameNotFoundException.class);
        }

        @Test
        @DisplayName("모든 데이터 있음 → CCU + 리뷰 % + Twitch + YouTube/Reddit 멘션 + lastUpdated")
        void allDataPresent() {
            when(gameRepo.existsById(1L)).thenReturn(true);
            Instant t1 = Instant.parse("2026-05-06T01:00:00Z");
            Instant t2 = Instant.parse("2026-05-06T02:00:00Z");
            Instant t3 = Instant.parse("2026-05-06T03:00:00Z");

            when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(snapshotWithReviews(100_000, 95_000, 100_000, t1)));
            when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                    .thenReturn(Optional.of(viewerSnapshot(50_000, t2)));
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(eq(1L), eq(SnapshotSource.YOUTUBE)))
                    .thenReturn(Optional.of(mentionSnapshot(12_345, SnapshotSource.YOUTUBE, t1)));
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(eq(1L), eq(SnapshotSource.REDDIT)))
                    .thenReturn(Optional.of(mentionSnapshot(50, SnapshotSource.REDDIT, t3)));

            PlayerInsight insight = service.getPlayerInsight(1L);

            assertThat(insight.gameId()).isEqualTo(1L);
            assertThat(insight.players().concurrentPlayers()).isEqualTo(100_000);
            assertThat(insight.players().reviewScorePositive()).isEqualTo(95_000);
            assertThat(insight.players().reviewScoreTotal()).isEqualTo(100_000);
            assertThat(insight.players().reviewScorePercent()).isEqualTo(95.0);
            assertThat(insight.twitchViewers()).isEqualTo(50_000);
            assertThat(insight.mentions()).hasSize(2);
            assertThat(insight.mentions())
                    .extracting(PlayerInsight.MentionByPlatform::source)
                    .containsExactlyInAnyOrder("YOUTUBE", "REDDIT");
            assertThat(insight.lastUpdated()).isEqualTo(t3); // t1, t2, t3 중 최신
        }

        @Test
        @DisplayName("PlayerSnapshot 없음 → players 모두 null, 다른 데이터는 정상")
        void noPlayerSnapshot() {
            when(gameRepo.existsById(1L)).thenReturn(true);
            when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L))
                    .thenReturn(Optional.of(viewerSnapshot(1_000, Instant.now())));
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                    .thenReturn(Optional.empty());

            PlayerInsight insight = service.getPlayerInsight(1L);

            assertThat(insight.players().concurrentPlayers()).isNull();
            assertThat(insight.players().reviewScorePositive()).isNull();
            assertThat(insight.players().reviewScoreTotal()).isNull();
            assertThat(insight.players().reviewScorePercent()).isNull();
            assertThat(insight.twitchViewers()).isEqualTo(1_000);
            assertThat(insight.mentions()).isEmpty();
        }

        @Test
        @DisplayName("리뷰 total=0 → percent null (DivisionByZero 회피)")
        void reviewTotalZero_percentNull() {
            when(gameRepo.existsById(1L)).thenReturn(true);
            when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(snapshotWithReviews(0, 0, 0, Instant.now())));
            when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                    .thenReturn(Optional.empty());

            PlayerInsight insight = service.getPlayerInsight(1L);

            assertThat(insight.players().reviewScoreTotal()).isEqualTo(0);
            assertThat(insight.players().reviewScorePercent()).isNull();
        }

        @Test
        @DisplayName("리뷰 percent 반올림 — 95234/100000 = 95.2%")
        void reviewPercent_rounded() {
            when(gameRepo.existsById(1L)).thenReturn(true);
            when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(snapshotWithReviews(0, 95_234, 100_000, Instant.now())));
            when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(anyLong(), any()))
                    .thenReturn(Optional.empty());

            PlayerInsight insight = service.getPlayerInsight(1L);

            assertThat(insight.players().reviewScorePercent()).isEqualTo(95.2);
        }

        @Test
        @DisplayName("일부 멘션만 있음 — Reddit만 응답, YouTube empty")
        void partialMentions() {
            when(gameRepo.existsById(1L)).thenReturn(true);
            when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(viewerRepo.findFirstByGameIdOrderByCapturedAtDesc(1L)).thenReturn(Optional.empty());
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(eq(1L), eq(SnapshotSource.YOUTUBE)))
                    .thenReturn(Optional.empty());
            when(mentionRepo.findFirstByGameIdAndSourceOrderByCapturedAtDesc(eq(1L), eq(SnapshotSource.REDDIT)))
                    .thenReturn(Optional.of(mentionSnapshot(42, SnapshotSource.REDDIT, Instant.now())));

            PlayerInsight insight = service.getPlayerInsight(1L);

            assertThat(insight.mentions()).hasSize(1);
            assertThat(insight.mentions().get(0).source()).isEqualTo("REDDIT");
            assertThat(insight.mentions().get(0).mentionCount()).isEqualTo(42);
        }
    }
}
