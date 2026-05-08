package com.gametrend.insight.infrastructure.persistence.watchlist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface WatchlistItemJpaRepository extends JpaRepository<WatchlistItemJpaEntity, Long> {

    List<WatchlistItemJpaEntity> findByUserIdOrderByAddedAtDesc(Long userId);

    boolean existsByUserIdAndGameId(Long userId, Long gameId);

    @Transactional
    long deleteByUserIdAndGameId(Long userId, Long gameId);
}
