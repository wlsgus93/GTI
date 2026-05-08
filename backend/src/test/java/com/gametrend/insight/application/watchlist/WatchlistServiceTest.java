package com.gametrend.insight.application.watchlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.watchlist.WatchlistItemJpaEntity;
import com.gametrend.insight.infrastructure.persistence.watchlist.WatchlistItemJpaRepository;
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
class WatchlistServiceTest {

    @Mock WatchlistItemJpaRepository watchlistRepo;
    @Mock GameJpaRepository gameRepo;
    @Mock PlayerSnapshotJpaRepository playerRepo;

    private WatchlistService service;

    @BeforeEach
    void setUp() {
        service = new WatchlistService(watchlistRepo, gameRepo, playerRepo);
    }

    @Test
    @DisplayName("list — 빈 워치리스트 → 빈 리스트")
    void list_empty() {
        when(watchlistRepo.findByUserIdOrderByAddedAtDesc(1L)).thenReturn(List.of());
        assertThat(service.list(1L)).isEmpty();
    }

    @Test
    @DisplayName("list — 게임 메타 + 최신 CCU 매핑")
    void list_withMeta() {
        WatchlistItemJpaEntity item = sampleItem(10L, 1L, 100L);
        when(watchlistRepo.findByUserIdOrderByAddedAtDesc(1L)).thenReturn(List.of(item));
        when(gameRepo.findAllById(List.of(100L))).thenReturn(List.of(sampleGame(100L, 730L, "CS2")));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(List.of(sampleSnapshot(1_100_000)));

        var list = service.list(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).name()).isEqualTo("CS2");
        assertThat(list.get(0).steamAppId()).isEqualTo(730L);
        assertThat(list.get(0).latestCcu()).isEqualTo(1_100_000);
    }

    @Test
    @DisplayName("add — 미존재 게임 → GameNotFoundException")
    void add_unknownGame() {
        when(gameRepo.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> service.add(1L, new AddWatchlistRequest(999L, null)))
                .isInstanceOf(GameNotFoundException.class);
    }

    @Test
    @DisplayName("add — 이미 워치리스트에 있는 게임 → IAE")
    void add_duplicate() {
        when(gameRepo.existsById(100L)).thenReturn(true);
        when(watchlistRepo.existsByUserIdAndGameId(1L, 100L)).thenReturn(true);
        assertThatThrownBy(() -> service.add(1L, new AddWatchlistRequest(100L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already in watchlist");
    }

    @Test
    @DisplayName("add — 정상 → 게임 메타 + CCU 포함 응답")
    void add_success() {
        when(gameRepo.existsById(100L)).thenReturn(true);
        when(watchlistRepo.existsByUserIdAndGameId(1L, 100L)).thenReturn(false);
        when(watchlistRepo.save(any())).thenAnswer(inv -> {
            WatchlistItemJpaEntity e = inv.getArgument(0);
            setField(e, "id", 99L);
            setField(e, "addedAt", Instant.now());
            return e;
        });
        when(gameRepo.findById(100L)).thenReturn(java.util.Optional.of(sampleGame(100L, 730L, "CS2")));
        when(playerRepo.findByGameIdOrderByCapturedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(List.of(sampleSnapshot(500_000)));

        var item = service.add(1L, new AddWatchlistRequest(100L, "Eyeing this one"));

        assertThat(item.id()).isEqualTo(99L);
        assertThat(item.gameId()).isEqualTo(100L);
        assertThat(item.note()).isEqualTo("Eyeing this one");
        assertThat(item.latestCcu()).isEqualTo(500_000);
    }

    @Test
    @DisplayName("remove — 워치리스트에 없으면 IAE")
    void remove_notInWatchlist() {
        when(watchlistRepo.deleteByUserIdAndGameId(1L, 100L)).thenReturn(0L);
        assertThatThrownBy(() -> service.remove(1L, 100L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in watchlist");
    }

    @Test
    @DisplayName("remove — 정상 삭제")
    void remove_success() {
        when(watchlistRepo.deleteByUserIdAndGameId(1L, 100L)).thenReturn(1L);
        service.remove(1L, 100L); // throws X
    }

    private static WatchlistItemJpaEntity sampleItem(long id, long userId, long gameId) {
        WatchlistItemJpaEntity e = WatchlistItemJpaEntity.newInstance(userId, gameId, "test note");
        setField(e, "id", id);
        setField(e, "addedAt", Instant.now());
        return e;
    }

    private static GameJpaEntity sampleGame(long id, long steamAppId, String name) {
        try {
            var ctor = GameJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            GameJpaEntity g = ctor.newInstance();
            setField(g, "id", id);
            g.setSteamAppId(steamAppId);
            g.setName(name);
            return g;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static PlayerSnapshotJpaEntity sampleSnapshot(int ccu) {
        try {
            var ctor = PlayerSnapshotJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            PlayerSnapshotJpaEntity s = ctor.newInstance();
            s.setConcurrentPlayers(ccu);
            s.setCapturedAt(Instant.now());
            return s;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setField(Object o, String name, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(o, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
