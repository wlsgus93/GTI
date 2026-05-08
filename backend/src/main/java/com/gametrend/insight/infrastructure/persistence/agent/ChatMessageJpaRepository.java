package com.gametrend.insight.infrastructure.persistence.agent;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessageJpaEntity, Long> {

    /** 세션의 최신 메시지 N건 (꼬리질문 컨텍스트용) — 시간 역순. */
    List<ChatMessageJpaEntity> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    long countBySessionId(Long sessionId);
}
