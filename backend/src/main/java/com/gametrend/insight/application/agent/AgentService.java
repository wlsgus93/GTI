package com.gametrend.insight.application.agent;

import com.gametrend.insight.application.agent.IntentClassifier.Classification;
import com.gametrend.insight.application.agent.IntentClassifier.Intent;
import com.gametrend.insight.application.agent.IntentClassifier.Topic;
import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import com.gametrend.insight.application.insight.LlmTask;
import com.gametrend.insight.domain.insight.Persona;
import com.gametrend.insight.infrastructure.agent.HardcodedSmallTalkFilter;
import com.gametrend.insight.infrastructure.persistence.agent.ChatMessageJpaEntity;
import com.gametrend.insight.infrastructure.persistence.agent.ChatMessageJpaRepository;
import com.gametrend.insight.infrastructure.persistence.agent.ChatSessionJpaEntity;
import com.gametrend.insight.infrastructure.persistence.agent.ChatSessionJpaRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 3-Layer 하이브리드 Agent 서비스 (룰 95).
 *
 * <p>흐름:
 * <ol>
 *   <li><b>Layer 1 — 분류</b> {@link IntentClassifier} (로컬 LLM 또는 hardcoded). OFF_TOPIC/SMALL_TALK 면 cloud 호출 X
 *   <li><b>Layer 2 — 컨텍스트</b> 세션 이력 (FOLLOW_UP/PERSONA_SWITCH 시 최근 N 메시지 messages[] 로 cloud 에 전달)
 *   <li><b>Layer 3 — Cloud LLM</b> {@link LlmClient} (Gemini Flash 등). 응답 + 토큰 사용량 DB 영속
 * </ol>
 *
 * <p><b>비용 차별점</b> (룰 95): 일 100 query 가정 → 30% Layer 1 차단 + 캐시 hit 50% = 실 호출 35건 → 토큰 65% 절감.
 *
 * <p><b>Compaction (Phase 5)</b>: 세션 메시지 6턴 넘으면 최근 6턴만 컨텍스트로 사용 (단순 truncation).
 * LLM 자체 요약은 향후 후속 (token 절약 vs 정보 보존 trade-off 평가 후).
 */
@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    /** 컨텍스트로 가져올 최근 메시지 수 (user + assistant 합산). */
    private static final int CONTEXT_TURN_LIMIT = 6;

    /** Cloud LLM 호출 max tokens (페르소나 인사이트). */
    private static final int CLOUD_MAX_TOKENS = 4096;

    private final IntentClassifier classifier;
    private final LlmClient llmClient;
    private final ChatSessionJpaRepository sessionRepo;
    private final ChatMessageJpaRepository messageRepo;

    public AgentService(IntentClassifier classifier, LlmClient llmClient,
            ChatSessionJpaRepository sessionRepo, ChatMessageJpaRepository messageRepo) {
        this.classifier = classifier;
        this.llmClient = llmClient;
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
    }

    @Transactional
    public AgentResponse handle(Long userId, AgentRequest request) {
        long startNs = System.nanoTime();

        // --- Step 1: 세션 ---
        ChatSessionJpaEntity session = resolveSession(userId, request);

        // --- Step 2: Layer 1 분류 (W9 옵션 C — 페르소나 시그널 자동 감지 포함) ---
        Classification c = classifier.classify(request.query());
        log.info("Layer 1 classification: topic={}, intent={}, confidence={}, inferredPersona={}",
                c.topic(), c.intent(), c.confidence(), c.inferredPersona());

        // W9 옵션 C — 페르소나 자동 추론 시 chat_session.persona 갱신 (UI 가 morph 트리거)
        boolean personaInferred = false;
        Persona previousPersona = session.getPersona();
        if (c.inferredPersona() != null && c.inferredPersona() != previousPersona) {
            session.setPersona(c.inferredPersona());
            personaInferred = true;
            log.info("Persona auto-inferred: {} → {}", previousPersona, c.inferredPersona());
        }

        // user 메시지 저장 (cloud 호출 여부와 무관)
        boolean willBlock = !c.shouldCallCloud();
        messageRepo.save(ChatMessageJpaEntity.userMessage(
                session.getId(), request.query(),
                c.topic(), c.intent(), c.confidence(), willBlock));

        // --- Step 3: Layer 1 차단 → hardcoded 응답 (cloud 호출 X) ---
        if (willBlock) {
            String content = hardcodedResponse(c.topic(), c.intent());
            ChatMessageJpaEntity assistantMsg = messageRepo.save(ChatMessageJpaEntity.assistantMessage(
                    session.getId(), content, List.of(), 0, 0, "hardcoded", false, 0));
            session.touch(2, 0);
            sessionRepo.save(session);
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            return AgentResponse.blocked(session.getId(), assistantMsg.getId(), content,
                    c.topic(), c.intent(), latencyMs, session.getPersona());
        }

        // --- Step 4: Layer 2 — 컨텍스트 ---
        List<ChatMessageJpaEntity> history = (c.intent() == Intent.FOLLOW_UP
                || c.intent() == Intent.PERSONA_SWITCH)
                ? messageRepo.findBySessionIdOrderByCreatedAtDesc(
                        session.getId(), PageRequest.of(0, CONTEXT_TURN_LIMIT))
                : List.of();

        // --- Step 5: Layer 3 — Cloud LLM (현재 세션 persona 로 톤 분기) ---
        String systemPrompt = buildSystemPrompt(session.getPersona(), history);
        LlmResponse llmResp;
        try {
            llmResp = llmClient.complete(LlmTask.PERSONA_INSIGHT, systemPrompt,
                    request.query(), CLOUD_MAX_TOKENS);
        } catch (RuntimeException e) {
            log.error("Cloud LLM call failed: {}", e.getMessage());
            String fallback = "죄송합니다. 분석 서버에 일시적 문제가 있습니다. 잠시 후 다시 시도해주세요.";
            ChatMessageJpaEntity assistantMsg = messageRepo.save(ChatMessageJpaEntity.assistantMessage(
                    session.getId(), fallback, List.of(), 0, 0, "fallback", false, 0));
            session.touch(2, 0);
            sessionRepo.save(session);
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000;
            return new AgentResponse(session.getId(), assistantMsg.getId(), fallback,
                    c.topic(), c.intent(), false, "fallback", 0, 0, false, latencyMs,
                    session.getPersona(), personaInferred);
        }

        long latencyMs = (System.nanoTime() - startNs) / 1_000_000;

        // --- Step 6: assistant 메시지 + 토큰 영속 ---
        ChatMessageJpaEntity assistantMsg = messageRepo.save(ChatMessageJpaEntity.assistantMessage(
                session.getId(), llmResp.content(), List.of(),
                llmResp.promptTokens(), llmResp.completionTokens(),
                llmResp.model(), false, (int) latencyMs));
        session.touch(2, llmResp.promptTokens() + llmResp.completionTokens());
        sessionRepo.save(session);

        return new AgentResponse(session.getId(), assistantMsg.getId(), llmResp.content(),
                c.topic(), c.intent(), false, llmResp.model(),
                llmResp.promptTokens(), llmResp.completionTokens(), false, latencyMs,
                session.getPersona(), personaInferred);
    }

    private ChatSessionJpaEntity resolveSession(Long userId, AgentRequest request) {
        if (request.sessionId() != null) {
            ChatSessionJpaEntity existing = sessionRepo.findById(request.sessionId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Session not found: " + request.sessionId()));
            if (!existing.getUserId().equals(userId)) {
                throw new IllegalArgumentException("Session ownership mismatch");
            }
            return existing;
        }
        Persona persona = request.persona() != null ? request.persona() : Persona.DEFAULT;
        return sessionRepo.save(ChatSessionJpaEntity.create(userId, persona, null));
    }

    private static String hardcodedResponse(Topic topic, Intent intent) {
        if (topic == Topic.SMALL_TALK) {
            return HardcodedSmallTalkFilter.smallTalkResponse();
        }
        if (intent == Intent.META) {
            return HardcodedSmallTalkFilter.metaResponse();
        }
        return HardcodedSmallTalkFilter.offTopicResponse();
    }

    /**
     * 페르소나 + 컨텍스트 (Compaction) 기반 system prompt 빌드.
     *
     * <p>Compaction (룰 95 §5.2): 6턴 limit. user/assistant 교대 message 누적. 최신순 → 시간순 reverse.
     */
    private static String buildSystemPrompt(Persona persona, List<ChatMessageJpaEntity> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 GTI(GameTrend-Insight) 게임 시장 분석가다. 페르소나: ")
                .append(persona.name()).append(" — ").append(persona.label())
                .append(" (").append(persona.tone()).append(").\n");
        sb.append("응답 구조: Evidence (수치+출처) → Insight (해석) → Strategy (실행안). 한국어 + 짧고 구체.\n");
        sb.append("수치는 DB Evidence 만 사용 — 추정은 명시 (\"약\", \"추정\"). 할루시네이션 금지.\n");

        if (!history.isEmpty()) {
            sb.append("\n<previous_context>\n");
            // 최신순 → 시간순 reverse
            List<ChatMessageJpaEntity> chronological = new ArrayList<>(history);
            Collections.reverse(chronological);
            for (ChatMessageJpaEntity m : chronological) {
                sb.append(m.getRole().name().toLowerCase()).append(": ")
                        .append(truncate(m.getContent(), 500)).append("\n");
            }
            sb.append("</previous_context>\n");
            sb.append("위 컨텍스트를 참고해 새 질문에 답하라. 꼬리질문이면 이전 게임/관점 유지.\n");
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
