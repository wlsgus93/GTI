package com.gametrend.insight.infrastructure.persistence.snapshot;

import com.gametrend.insight.domain.snapshot.SnapshotSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentionSnapshotJpaRepository extends JpaRepository<MentionSnapshotJpaEntity, Long> {

    List<MentionSnapshotJpaEntity> findByGameIdOrderByCapturedAtDesc(Long gameId, Pageable pageable);

    /** 특정 source의 가장 최근 1건 (P2 플레이어 분석 탭의 source별 최신 멘션 카운트). */
    Optional<MentionSnapshotJpaEntity> findFirstByGameIdAndSourceOrderByCapturedAtDesc(
            Long gameId, SnapshotSource source);

    /** D5 커뮤니티 활성도 — 게임의 모든 mention snapshot. */
    List<MentionSnapshotJpaEntity> findByGameId(Long gameId);

    /** D5 — 모든 게임의 총 mention count (Z-score 활성도 점수 입력). */
    @org.springframework.data.jpa.repository.Query("""
            SELECT m.gameId, SUM(m.mentionCount)
            FROM MentionSnapshotJpaEntity m
            GROUP BY m.gameId
            """)
    List<Object[]> sumMentionsByGame();
}
