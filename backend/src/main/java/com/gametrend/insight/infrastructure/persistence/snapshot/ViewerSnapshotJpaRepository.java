package com.gametrend.insight.infrastructure.persistence.snapshot;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ViewerSnapshotJpaRepository extends JpaRepository<ViewerSnapshotJpaEntity, Long> {

    List<ViewerSnapshotJpaEntity> findByGameIdOrderByCapturedAtDesc(Long gameId, Pageable pageable);

    /** P2 플레이어 분석 탭에서 최신 시청자 1건. */
    Optional<ViewerSnapshotJpaEntity> findFirstByGameIdOrderByCapturedAtDesc(Long gameId);
}
