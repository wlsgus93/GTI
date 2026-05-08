package com.gametrend.insight.infrastructure.persistence.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerSnapshotJpaRepository extends JpaRepository<PlayerSnapshotJpaEntity, Long> {

    /** 가장 최근 N개 (TrendQueryService에서 사용). */
    List<PlayerSnapshotJpaEntity> findByGameIdOrderByCapturedAtDesc(Long gameId, Pageable pageable);

    /** 시점 이후 모든 스냅샷, 시간 순 (CCU 차트). 인덱스 (game_id, captured_at desc) 활용. */
    List<PlayerSnapshotJpaEntity> findByGameIdAndCapturedAtAfterOrderByCapturedAtAsc(
            Long gameId, Instant capturedAtAfter);

    /** SteamSpy 출처 최신 1건 (owners 범위 보장). Economics 모듈에서 사용. */
    @Query("""
            SELECT s FROM PlayerSnapshotJpaEntity s
            WHERE s.gameId = :gameId
              AND s.ownersLow IS NOT NULL
              AND s.ownersHigh IS NOT NULL
            ORDER BY s.capturedAt DESC
            LIMIT 1
            """)
    Optional<PlayerSnapshotJpaEntity> findLatestWithOwners(@Param("gameId") Long gameId);

    /** 윈도우 내 최대 CCU. Economics DAU 추정 입력. */
    @Query("""
            SELECT MAX(s.concurrentPlayers) FROM PlayerSnapshotJpaEntity s
            WHERE s.gameId = :gameId
              AND s.capturedAt >= :since
              AND s.concurrentPlayers IS NOT NULL
            """)
    Integer findPeakCcuSince(@Param("gameId") Long gameId, @Param("since") Instant since);
}
