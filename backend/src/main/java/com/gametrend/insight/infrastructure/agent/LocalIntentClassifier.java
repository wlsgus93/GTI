package com.gametrend.insight.infrastructure.agent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.insight.application.agent.IntentClassifier;
import com.gametrend.insight.domain.insight.Persona;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 로컬 LLM 기반 분류기 (Layer 1 of 3-Layer 하이브리드).
 *
 * <p>모델: Llama 3.2 1B (Q4_K_M, ~700MB, CPU 1-2초). Ollama REST 호출.
 *
 * <p>흐름:
 * <ol>
 *   <li>{@link HardcodedSmallTalkFilter} — 정규식/토큰 매칭 (즉시, 토큰 0)
 *   <li>local LLM JSON classification — 프롬프트로 topic + intent 추출
 *   <li>JSON 파싱 실패 시 휴리스틱 fallback (UNCLEAR + 안전한 PASS)
 * </ol>
 *
 * <p>비용 차별점: cloud LLM 호출 직전에 차단 → 일 100 query 중 30% 차단 → 토큰 65% 절감.
 */
public final class LocalIntentClassifier implements IntentClassifier {

    private static final Logger log = LoggerFactory.getLogger(LocalIntentClassifier.class);

    private static final String SYSTEM_PROMPT = """
            너는 게임 시장 분석 도구의 의도 분류기다. 사용자 질문을 보고 JSON 한 줄만 출력해라.

            출력 schema:
            {"topic":"GAME|OFF_TOPIC|SMALL_TALK","intent":"NEW_QUERY|FOLLOW_UP|PERSONA_SWITCH|META|UNCLEAR","confidence":0.0~1.0,"persona":"INDIE|PUBLISHER|MARKETER|INVESTOR|null"}

            분류 기준:
            - topic=GAME: 게임/게임시장/게임산업/장르/플레이어수/매출/마케팅 관련
            - topic=OFF_TOPIC: 정치/요리/일반상식/날씨 등 게임 외
            - topic=SMALL_TALK: 인사/사례/감정표현
            - intent=NEW_QUERY: 새 분석 요청 ("CS2 분석해줘")
            - intent=FOLLOW_UP: 이전 응답에 추가 질문 ("그럼 vs 발로란트?")
            - intent=PERSONA_SWITCH: 관점 변경 ("인디 개발자 입장에서는?")
            - intent=META: 도구 사용법 질문 ("어떻게 사용해?")
            - intent=UNCLEAR: 분류 모호

            persona 시그널 (W9 옵션 C — Agentic UX):
            - INDIE: "내 게임", "우리 인디", "차별화", "1인 개발", "indie", "my game"
            - PUBLISHER: "포트폴리오", "퍼블리싱", "IP", "라이센싱", "다음 출시작"
            - MARKETER: "캠페인", "광고", "ROI", "CTR/CVR/CPV", "유입", "채널"
            - INVESTOR: "투자", "리스크", "성공 확률", "BEP", "수익", "벤처"
            - null: 페르소나 시그널 없음 (모호한 일반 query)

            JSON 만 출력. 설명 X.
            """;

    private final WebClient ollamaWebClient;
    private final String model;
    private final ObjectMapper objectMapper;

    public LocalIntentClassifier(WebClient ollamaWebClient, String model, ObjectMapper objectMapper) {
        this.ollamaWebClient = ollamaWebClient;
        this.model = model;
        this.objectMapper = objectMapper;
    }

    @Override
    public Classification classify(String userQuery) {
        // Step 0: hardcoded persona signal (W9 옵션 C — fast-path)
        Persona signaled = HardcodedSmallTalkFilter.detectPersonaSignal(userQuery);

        // Step 1: hardcoded fast-path (LLM 호출 X)
        if (HardcodedSmallTalkFilter.isSmallTalk(userQuery)) {
            return new Classification(Topic.SMALL_TALK, Intent.UNCLEAR, 1.0, "hardcoded smalltalk match", null);
        }
        if (HardcodedSmallTalkFilter.isMeta(userQuery)) {
            return new Classification(Topic.GAME, Intent.META, 1.0, "meta keyword match", null);
        }

        // Step 2: local LLM JSON classification
        try {
            Classification llm = classifyWithLocalLlm(userQuery);
            // hardcoded signal > LLM (정확도 ↑)
            if (signaled != null && llm.inferredPersona() == null) {
                return new Classification(llm.topic(), llm.intent(), llm.confidence(),
                        llm.reason() + " + hardcoded persona", signaled);
            }
            return llm;
        } catch (RuntimeException e) {
            log.warn("Local classifier failed (fallback to UNCLEAR PASS): {}", e.getMessage());
            // 분류기 fail 시 → 보수적으로 GAME + UNCLEAR (cloud 로 보내되 호출자가 추가 검증 가능)
            return new Classification(Topic.GAME, Intent.UNCLEAR, 0.3, "classifier fallback", signaled);
        }
    }

    private Classification classifyWithLocalLlm(String userQuery) {
        OllamaChatRequest req = new OllamaChatRequest(
                model,
                List.of(
                        new OllamaMessage("system", SYSTEM_PROMPT),
                        new OllamaMessage("user", userQuery)),
                false,
                Map.of("num_predict", 100, "temperature", 0.1));

        OllamaChatResponse resp = ollamaWebClient.post()
                .uri("/api/chat")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(OllamaChatResponse.class)
                .block();

        if (resp == null || resp.message() == null || resp.message().content() == null) {
            throw new IllegalStateException("Local classifier empty response");
        }
        String raw = resp.message().content().trim();
        // 일부 모델은 ```json ... ``` 으로 감쌈 — 추출
        String json = extractJson(raw);

        try {
            Map<String, Object> parsed = objectMapper.readValue(json, Map.class);
            Topic topic = parseTopic((String) parsed.get("topic"));
            Intent intent = parseIntent((String) parsed.get("intent"));
            double confidence = parsed.get("confidence") instanceof Number n ? n.doubleValue() : 0.5;
            Persona persona = parsePersona((String) parsed.get("persona"));
            return new Classification(topic, intent, confidence, "local LLM", persona);
        } catch (Exception e) {
            log.warn("JSON parse failed for classifier output: {} — raw: {}", e.getMessage(), raw);
            return new Classification(Topic.GAME, Intent.UNCLEAR, 0.3, "json parse failed", null);
        }
    }

    private static Persona parsePersona(String s) {
        if (s == null || s.isBlank() || "null".equalsIgnoreCase(s.trim())) return null;
        try {
            return Persona.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    private static Topic parseTopic(String s) {
        if (s == null) return Topic.GAME;
        try {
            return Topic.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Topic.GAME;
        }
    }

    private static Intent parseIntent(String s) {
        if (s == null) return Intent.UNCLEAR;
        try {
            return Intent.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Intent.UNCLEAR;
        }
    }

    // === Ollama REST schema (재사용 가능 — OllamaLlmClient 와 동일 구조) ===

    public record OllamaChatRequest(
            String model, List<OllamaMessage> messages, boolean stream, Map<String, Object> options) {}

    public record OllamaMessage(String role, String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OllamaChatResponse(String model, OllamaMessage message, Boolean done) {}
}
