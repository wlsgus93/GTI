package com.gametrend.insight.application.watchlist;

import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.PlayerSnapshotJpaRepository;
import com.gametrend.insight.infrastructure.persistence.watchlist.WatchlistItemJpaEntity;
import com.gametrend.insight.infrastructure.persistence.watchlist.WatchlistItemJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P4 워치리스트 — 사용자 게임 추적.
 */
@Service
public class WatchlistService {

    private final WatchlistItemJpaRepository watchlistRepo;
    private final GameJpaRepository gameRepo;
    private final PlayerSnapshotJpaRepository playerRepo;

    public WatchlistService(
            WatchlistItemJpaRepository watchlistRepo,
            GameJpaRepository gameRepo,
            PlayerSnapshotJpaRepository playerRepo) {
        this.watchlistRepo = watchlistRepo;
        this.gameRepo = gameRepo;
        this.playerRepo = playerRepo;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItem> list(long userId) {
        List<WatchlistItemJpaEntity> items = watchlistRepo.findByUserIdOrderByAddedAtDesc(userId);
        if (items.isEmpty()) return List.of();

        // 게임 메타 일괄 조회
        List<Long> gameIds = items.stream().map(WatchlistItemJpaEntity::getGameId).toList();
        Map<Long, GameJpaEntity> gameMap = gameRepo.findAllById(gameIds).stream()
                .collect(Collectors.toMap(GameJpaEntity::getId, g -> g));

        return items.stream()
                .map(item -> {
                    GameJpaEntity g = gameMap.get(item.getGameId());
                    Integer ccu = latestCcu(item.getGameId());
                    return new WatchlistItem(
                            item.getId(),
                            item.getGameId(),
                            g == null ? null : g.getSteamAppId(),
                            g == null ? "(deleted)" : g.getName(),
                            g == null ? null : g.getCoverImageUrl(),
                            ccu,
                            item.getNote(),
                            item.getAddedAt());
                })
                .toList();
    }

    @Transactional
    public WatchlistItem add(long userId, AddWatchlistRequest req) {
        if (!gameRepo.existsById(req.gameId())) {
            throw new GameNotFoundException(req.gameId());
        }
        if (watchlistRepo.existsByUserIdAndGameId(userId, req.gameId())) {
            throw new IllegalArgumentException(
                    "Game already in watchlist: gameId=" + req.gameId());
        }

        var entity = WatchlistItemJpaEntity.newInstance(userId, req.gameId(), req.note());
        var saved = watchlistRepo.save(entity);

        GameJpaEntity g = gameRepo.findById(req.gameId()).orElseThrow();
        return new WatchlistItem(
                saved.getId(),
                saved.getGameId(),
                g.getSteamAppId(),
                g.getName(),
                g.getCoverImageUrl(),
                latestCcu(req.gameId()),
                saved.getNote(),
                saved.getAddedAt());
    }

    @Transactional
    public void remove(long userId, long gameId) {
        long deleted = watchlistRepo.deleteByUserIdAndGameId(userId, gameId);
        if (deleted == 0) {
            throw new IllegalArgumentException(
                    "Game not in watchlist: gameId=" + gameId);
        }
    }

    private Integer latestCcu(long gameId) {
        return playerRepo.findByGameIdOrderByCapturedAtDesc(gameId, PageRequest.of(0, 1))
                .stream()
                .map(PlayerSnapshotJpaEntity::getConcurrentPlayers)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }
}
