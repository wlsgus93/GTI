package com.gametrend.insight.infrastructure.persistence.insight;

import com.gametrend.insight.domain.insight.AnalysisKind;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisJpaRepository extends JpaRepository<AnalysisJpaEntity, Long> {

    /**
     * 가장 최근 unexpired analysis 1건 (TTL 캐시 hit 판정).
     * promptVersion 일치까지 강제 — 프롬프트 변경 시 자동 무효.
     */
    @Query("""
            SELECT a FROM AnalysisJpaEntity a
            WHERE a.gameId = :gameId
              AND a.kind = :kind
              AND a.promptVersion = :promptVersion
              AND a.expiresAt > :now
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    Optional<AnalysisJpaEntity> findLatestFresh(
            @Param("gameId") Long gameId,
            @Param("kind") AnalysisKind kind,
            @Param("promptVersion") String promptVersion,
            @Param("now") Instant now);

    /**
     * TTL 무관 가장 최근 1건 — LLM 장애 시 stale fallback 용 (W3 D1).
     * promptVersion 매칭은 유지 (V1 응답을 V2 호출에 반환하지 않도록).
     */
    @Query("""
            SELECT a FROM AnalysisJpaEntity a
            WHERE a.gameId = :gameId
              AND a.kind = :kind
              AND a.promptVersion = :promptVersion
            ORDER BY a.createdAt DESC
            LIMIT 1
            """)
    Optional<AnalysisJpaEntity> findLatestAny(
            @Param("gameId") Long gameId,
            @Param("kind") AnalysisKind kind,
            @Param("promptVersion") String promptVersion);
}
