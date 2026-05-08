package com.gametrend.insight.application.trend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TrendQueryServiceTest {

    @Mock
    private GameJpaRepository gameRepo;

    @Mock
    private PlayerSnapshotJpaRepository snapshotRepo;

    @Mock
    private com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate redisCache;

    private TrendQueryService service;

    @BeforeEach
    void setUp() {
        service = new TrendQueryService(gameRepo, snapshotRepo, new TrendScoreCalculator(), redisCache);
        // 기본: 캐시 miss
        org.mockito.Mockito.lenient()
                .when(redisCache.get(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.eq(TrendQueryService.CachedTrendBoard.class)))
                .thenReturn(java.util.Optional.empty());
    }

    @Test
    @DisplayName("게임 0개 → 빈 리스트")
    void empty_returnsEmpty() {
        when(gameRepo.findAll()).thenReturn(List.of());
        assertThat(service.topByTrendScore(50)).isEmpty();
    }

    @Test
    @DisplayName("Top N 정렬: CCU 큰 순서로, score 100~0 범위, deltaPct 계산됨")
    void top_sortsByScore() {
        GameJpaEntity g1 = makeGame(1L, 730L, "Counter-Strike 2");
        GameJpaEntity g2 = makeGame(2L, 570L, "Dota 2");
        GameJpaEntity g3 = makeGame(3L, 1245620L, "ELDEN RING");
        when(gameRepo.findAll()).thenReturn(List.of(g3, g1, g2)); // 의도적 비순서

        // CS2 (g1): 1.1M (현재) / 1.05M (이전) → +4.7%
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(1_100_000), snapshot(1_050_000)));
        // Dota2 (g2): 700k / 720k → -2.8%
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(2L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(700_000), snapshot(720_000)));
        // ELDEN RING (g3): 200k / 185k → +8.1%
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(3L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(200_000), snapshot(185_000)));

        List<TrendBoardItem> top = service.topByTrendScore(50);

        assertThat(top).hasSize(3);
        // 정렬: CS2 > Dota2 > ELDEN RING (CCU 절대값 기준)
        assertThat(top.get(0).title()).isEqualTo("Counter-Strike 2");
        assertThat(top.get(0).trendScore()).isEqualTo(100.0); // maxCcu와 동일
        assertThat(top.get(0).ccuDeltaPct()).isCloseTo(4.8, org.assertj.core.api.Assertions.within(0.2));

        assertThat(top.get(1).title()).isEqualTo("Dota 2");
        assertThat(top.get(1).trendScore()).isLessThan(100.0).isGreaterThan(95.0);
        assertThat(top.get(1).ccuDeltaPct()).isLessThan(0.0); // 감소

        assertThat(top.get(2).title()).isEqualTo("ELDEN RING");
        assertThat(top.get(2).trendScore()).isLessThan(top.get(1).trendScore());
        assertThat(top.get(2).ccuDeltaPct()).isGreaterThan(0.0); // 증가
    }

    @Test
    @DisplayName("스냅샷 1건만 있으면 deltaPct null, score는 정상 계산")
    void onlyOneSnapshot_deltaIsNull() {
        GameJpaEntity g = makeGame(1L, 730L, "CS2");
        when(gameRepo.findAll()).thenReturn(List.of(g));
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(500_000)));

        List<TrendBoardItem> top = service.topByTrendScore(50);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).ccuDeltaPct()).isNull();
        assertThat(top.get(0).trendScore()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Redis L1 hit — DB 호출 X (W3 D3)")
    void redisHit_skipsDb() {
        var cached = new TrendQueryService.CachedTrendBoard(List.of(
                new TrendBoardItem("730", "Counter-Strike 2", "Game", "Steam", 100.0, 4.7, 1_100_000)));
        when(redisCache.get(eq("trends:limit:50"), eq(TrendQueryService.CachedTrendBoard.class)))
                .thenReturn(java.util.Optional.of(cached));

        List<TrendBoardItem> top = service.topByTrendScore(50);

        assertThat(top).hasSize(1);
        assertThat(top.get(0).title()).isEqualTo("Counter-Strike 2");
        org.mockito.Mockito.verify(gameRepo, org.mockito.Mockito.never()).findAll();
        org.mockito.Mockito.verify(redisCache, org.mockito.Mockito.never())
                .put(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Redis miss → DB 조회 + 결과 캐시 (W3 D3)")
    void redisMiss_computesAndCaches() {
        when(redisCache.get(eq("trends:limit:50"), eq(TrendQueryService.CachedTrendBoard.class)))
                .thenReturn(java.util.Optional.empty());
        when(gameRepo.findAll()).thenReturn(List.of(makeGame(1L, 730L, "CS2")));
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(snapshot(100_000)));

        service.topByTrendScore(50);

        org.mockito.Mockito.verify(redisCache, org.mockito.Mockito.times(1))
                .put(eq("trends:limit:50"),
                        org.mockito.ArgumentMatchers.any(TrendQueryService.CachedTrendBoard.class),
                        eq(TrendQueryService.CACHE_TTL));
    }

    @Test
    @DisplayName("limit 적용: 5개 게임 중 limit=2 → 2개 반환")
    void limit_capsResults() {
        when(gameRepo.findAll()).thenReturn(List.of(
                makeGame(1L, 730L, "g1"), makeGame(2L, 570L, "g2"), makeGame(3L, 100L, "g3"),
                makeGame(4L, 200L, "g4"), makeGame(5L, 300L, "g5")));
        when(snapshotRepo.findByGameIdOrderByCapturedAtDesc(anyLong(), any(Pageable.class)))
                .thenReturn(List.of(snapshot(10_000)));

        assertThat(service.topByTrendScore(2)).hasSize(2);
    }

    private static GameJpaEntity makeGame(long id, long steamAppId, String name) {
        try {
            var ctor = GameJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            GameJpaEntity g = ctor.newInstance();
            Field f = GameJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(g, id);
            g.setSteamAppId(steamAppId);
            g.setName(name);
            return g;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static PlayerSnapshotJpaEntity snapshot(int ccu) {
        try {
            var ctor = PlayerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            PlayerSnapshotJpaEntity s = ctor.newInstance();
            s.setConcurrentPlayers(ccu);
            s.setCapturedAt(Instant.now());
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
