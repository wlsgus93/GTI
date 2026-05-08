package com.gametrend.insight.infrastructure.persistence.agent;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatSessionJpaRepository extends JpaRepository<ChatSessionJpaEntity, Long> {

    @Query("""
            SELECT s FROM ChatSessionJpaEntity s
            WHERE s.userId = :userId AND s.closedAt IS NULL
            ORDER BY s.lastActiveAt DESC
            """)
    List<ChatSessionJpaEntity> findActiveByUser(@Param("userId") Long userId);
}
