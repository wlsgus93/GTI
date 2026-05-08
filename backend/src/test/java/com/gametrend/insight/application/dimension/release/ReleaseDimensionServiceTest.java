package com.gametrend.insight.application.dimension.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.game.GenreJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ReleaseDimensionServiceTest {

    @Mock GameJpaRepository gameRepo;
    @Mock PlayerSnapshotJpaRepository playerRepo;
    @Mock RedisCacheTemplate redisCache;

    private ReleaseDimensionService service;

    @BeforeEach
    void setUp() {
        var classifier = new com.gametrend.insight.application.stats.HitFlopClassifier(
                new com.gametrend.insight.application.stats.ZScoreNormalizer());
        service = new ReleaseDimensionService(gameRepo, playerRepo, redisCache, classifier);
        Mockito.lenient()
                .when(redisCache.get(org.mockito.ArgumentMatchers.anyString(),
                        eq(ReleaseDimension.class)))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("게임 0개 → 빈 결과")
    void empty() {
        when(gameRepo.findAll()).thenReturn(List.of());
        var result = service.getReleaseDimension();
        assertThat(result.totalGames()).isEqualTo(0);
        assertThat(result.byGenre()).isEmpty();
        assertThat(result.byYear()).isEmpty();
    }

    @Test
    @DisplayName("Redis hit → 즉시 반환, DB 호출 X")
    void redisHit() {
        var cached = new ReleaseDimension(List.of(), List.of(), 0, Instant.now());
        when(redisCache.get(org.mockito.ArgumentMatchers.anyString(), eq(ReleaseDimension.class)))
                .thenReturn(Optional.of(cached));

        service.getReleaseDimension();

        Mockito.verify(gameRepo, Mockito.never()).findAll();
    }

    @Test
    @DisplayName("장르별 집계 — 동일 장르 게임 N개 → gameCount=N, avg/max CCU 정확")
    void byGenre_aggregates() {
        // CS2 (Action, ccu 1100K), Dota2 (Action, 700K), Elden Ring (RPG+Action, 200K)
        GameJpaEntity cs2 = makeGame(1L, 730L, "CS2", LocalDate.of(2023, 9, 27), Set.of("Action"));
        GameJpaEntity dota = makeGame(2L, 570L, "Dota 2", LocalDate.of(2013, 7, 9), Set.of("Action"));
        GameJpaEntity elden = makeGame(3L, 1245620L, "Elden Ring", LocalDate.of(2022, 2, 25), Set.of("RPG", "Action"));

        when(gameRepo.findAll()).thenReturn(List.of(cs2, dota, elden));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(snapshot(1_100_000)));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(2L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(snapshot(700_000)));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(3L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(snapshot(200_000)));

        var result = service.getReleaseDimension();

        assertThat(result.totalGames()).isEqualTo(3);
        // Action: 3 게임 (CS2, Dota2, Elden Ring)
        var action = result.byGenre().stream()
                .filter(g -> g.genre().equals("Action"))
                .findFirst().orElseThrow();
        assertThat(action.gameCount()).isEqualTo(3);
        assertThat(action.avgLatestCcu()).isEqualTo(666_666); // (1100K+700K+200K)/3
        assertThat(action.maxLatestCcu()).isEqualTo(1_100_000);
        assertThat(action.topGameName()).isEqualTo("CS2");
        // RPG: 1 게임 (Elden Ring만)
        var rpg = result.byGenre().stream()
                .filter(g -> g.genre().equals("RPG"))
                .findFirst().orElseThrow();
        assertThat(rpg.gameCount()).isEqualTo(1);
        assertThat(rpg.topGameName()).isEqualTo("Elden Ring");
        // 정렬: gameCount desc → Action(3) > RPG(1)
        assertThat(result.byGenre().get(0).genre()).isEqualTo("Action");
    }

    @Test
    @DisplayName("연도별 집계 — release_date 기준 그루핑, 최근 연도부터")
    void byYear_groupsAndSorts() {
        GameJpaEntity g2023 = makeGame(1L, 730L, "CS2", LocalDate.of(2023, 9, 27), Set.of("Action"));
        GameJpaEntity g2020a = makeGame(2L, 1145360L, "Hades", LocalDate.of(2020, 9, 17), Set.of("Indie"));
        GameJpaEntity g2020b = makeGame(3L, 1091500L, "Cyberpunk", LocalDate.of(2020, 12, 10), Set.of("RPG"));

        when(gameRepo.findAll()).thenReturn(List.of(g2023, g2020a, g2020b));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(anyLong(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(snapshot(50_000)));

        var result = service.getReleaseDimension();

        assertThat(result.byYear()).hasSize(2);
        assertThat(result.byYear().get(0).year()).isEqualTo(2023);
        assertThat(result.byYear().get(0).gameCount()).isEqualTo(1);
        assertThat(result.byYear().get(1).year()).isEqualTo(2020);
        assertThat(result.byYear().get(1).gameCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("release_date null인 게임은 byYear에서 제외, byGenre에는 포함")
    void nullReleaseDate_excludedFromYear() {
        GameJpaEntity withDate = makeGame(1L, 730L, "CS2", LocalDate.of(2023, 1, 1), Set.of("Action"));
        GameJpaEntity noDate = makeGame(2L, 999L, "Unknown", null, Set.of("Action"));
        when(gameRepo.findAll()).thenReturn(List.of(withDate, noDate));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(anyLong(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(snapshot(100_000)));

        var result = service.getReleaseDimension();

        assertThat(result.byYear()).hasSize(1); // 2023만
        assertThat(result.byGenre().get(0).gameCount()).isEqualTo(2); // Action 2개
    }

    @Test
    @DisplayName("CCU 스냅샷 없는 게임 — gameCount은 카운트, avg는 nonNull만")
    void noSnapshot_gameCountedButCcuExcluded() {
        GameJpaEntity g1 = makeGame(1L, 730L, "CS2", LocalDate.of(2023, 1, 1), Set.of("Action"));
        GameJpaEntity g2 = makeGame(2L, 999L, "ObscureGame", LocalDate.of(2023, 6, 1), Set.of("Action"));
        when(gameRepo.findAll()).thenReturn(List.of(g1, g2));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(1L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(snapshot(100_000)));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(2L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of()); // 빈 결과

        var result = service.getReleaseDimension();

        var action = result.byGenre().get(0);
        assertThat(action.gameCount()).isEqualTo(2);
        assertThat(action.avgLatestCcu()).isEqualTo(100_000); // CS2만
        assertThat(action.topGameName()).isEqualTo("CS2");
    }

    @Test
    @DisplayName("Redis miss → 결과 30분 TTL로 캐시")
    void redisMiss_caches() {
        when(gameRepo.findAll()).thenReturn(List.of());

        service.getReleaseDimension();

        Mockito.verify(redisCache, Mockito.times(1))
                .put(eq("dim:d1:release:v1"),
                        org.mockito.ArgumentMatchers.any(ReleaseDimension.class),
                        eq(ReleaseDimensionService.CACHE_TTL));
    }

    private static GameJpaEntity makeGame(long id, long steamAppId, String name, LocalDate release, Set<String> genres) {
        try {
            var ctor = GameJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            GameJpaEntity g = ctor.newInstance();
            Field f = GameJpaEntity.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(g, id);
            g.setSteamAppId(steamAppId);
            g.setName(name);
            g.setReleaseDate(release);
            // Genres
            Set<GenreJpaEntity> genreSet = new HashSet<>();
            long gid = 1;
            for (String name2 : genres) {
                var gctor = GenreJpaEntity.class.getDeclaredConstructor();
                gctor.setAccessible(true);
                GenreJpaEntity gn = gctor.newInstance();
                Field gf = GenreJpaEntity.class.getDeclaredField("id");
                gf.setAccessible(true);
                gf.set(gn, gid++);
                gn.setName(name2);
                genreSet.add(gn);
            }
            g.setGenres(genreSet);
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
