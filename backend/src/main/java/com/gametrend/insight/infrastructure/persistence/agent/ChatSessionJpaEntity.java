package com.gametrend.insight.infrastructure.persistence.agent;

import com.gametrend.insight.domain.agent.ChatSession;
import com.gametrend.insight.domain.insight.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_session")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatSessionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Persona persona;

    @Column(length = 200)
    private String title;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    @Column(name = "total_messages", nullable = false)
    private int totalMessages;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "closed_at")
    private Instant closedAt;

    public ChatSession toDomain() {
        return new ChatSession(id, userId, persona, title, startedAt, lastActiveAt,
                totalMessages, totalTokens, closedAt);
    }

    public static ChatSessionJpaEntity create(Long userId, Persona persona, String title) {
        ChatSessionJpaEntity e = new ChatSessionJpaEntity();
        e.userId = userId;
        e.persona = persona;
        e.title = title;
        e.startedAt = Instant.now();
        e.lastActiveAt = Instant.now();
        return e;
    }

    public void touch(int addedMessages, int addedTokens) {
        this.totalMessages += addedMessages;
        this.totalTokens += addedTokens;
        this.lastActiveAt = Instant.now();
    }
}
