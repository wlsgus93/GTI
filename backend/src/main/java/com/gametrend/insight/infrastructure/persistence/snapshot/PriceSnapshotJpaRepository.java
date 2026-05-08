package com.gametrend.insight.infrastructure.persistence.snapshot;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceSnapshotJpaRepository extends JpaRepository<PriceSnapshotJpaEntity, Long> {

    @Query("""
            SELECT s FROM PriceSnapshotJpaEntity s
            WHERE s.gameId = :gameId
              AND s.capturedAt >= :from
              AND s.capturedAt <= :to
            ORDER BY s.capturedAt DESC
            """)
    List<PriceSnapshotJpaEntity> findByGameIdAndCapturedAtBetween(
            @Param("gameId") Long gameId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    List<PriceSnapshotJpaEntity> findByGameIdOrderByCapturedAtDesc(Long gameId, Pageable pageable);

    /** Economics 모듈에서 최신 가격 1건. */
    Optional<PriceSnapshotJpaEntity> findFirstByGameIdOrderByCapturedAtDesc(Long gameId);
}
