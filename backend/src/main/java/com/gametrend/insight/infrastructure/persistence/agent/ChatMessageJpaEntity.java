package com.gametrend.insight.infrastructure.persistence.agent;

import com.gametrend.insight.application.agent.IntentClassifier.Intent;
import com.gametrend.insight.application.agent.IntentClassifier.Topic;
import com.gametrend.insight.domain.agent.ChatMessage;
import com.gametrend.insight.domain.agent.ChatRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "chat_message")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Layer 1 분류 결과 (USER 메시지만 — 그 외 null)
    @Enumerated(EnumType.STRING)
    @Column(name = "classified_topic", length = 20)
    private Topic classifiedTopic;

    @Enumerated(EnumType.STRING)
    @Column(name = "classified_intent", length = 30)
    private Intent classifiedIntent;

    @Column(name = "classified_confidence")
    private Double classifiedConfidence;

    @Column(name = "classifier_blocked", nullable = false)
    private boolean classifierBlocked;

    // Layer 3 cloud LLM 메타 (ASSISTANT 메시지만)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "referenced_game_ids", columnDefinition = "BIGINT[]")
    private Long[] referencedGameIds;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(length = 50)
    private String model;

    @Column(nullable = false)
    private boolean cached;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public ChatMessage toDomain() {
        List<Long> games = referencedGameIds == null ? List.of() : Arrays.asList(referencedGameIds);
        return new ChatMessage(id, sessionId, role, content,
                classifiedTopic, classifiedIntent, classifiedConfidence, classifierBlocked,
                games, promptTokens, completionTokens, model, cached, latencyMs, createdAt);
    }

    public static ChatMessageJpaEntity userMessage(Long sessionId, String content,
            Topic topic, Intent intent, Double confidence, boolean blocked) {
        ChatMessageJpaEntity e = new ChatMessageJpaEntity();
        e.sessionId = sessionId;
        e.role = ChatRole.USER;
        e.content = content;
        e.classifiedTopic = topic;
        e.classifiedIntent = intent;
        e.classifiedConfidence = confidence;
        e.classifierBlocked = blocked;
        e.cached = false;
        e.createdAt = Instant.now();
        return e;
    }

    public static ChatMessageJpaEntity assistantMessage(Long sessionId, String content,
            List<Long> referencedGameIds, Integer promptTokens, Integer completionTokens,
            String model, boolean cached, Integer latencyMs) {
        ChatMessageJpaEntity e = new ChatMessageJpaEntity();
        e.sessionId = sessionId;
        e.role = ChatRole.ASSISTANT;
        e.content = content;
        e.referencedGameIds = referencedGameIds == null
                ? null
                : referencedGameIds.stream().collect(Collectors.toList()).toArray(new Long[0]);
        e.promptTokens = promptTokens;
        e.completionTokens = completionTokens;
        e.model = model;
        e.cached = cached;
        e.latencyMs = latencyMs;
        e.classifierBlocked = false;
        e.createdAt = Instant.now();
        return e;
    }
}
