package com.gametrend.insight.application.insight;

/**
 * LLM 호출 task 분류 — RoutingLlmClient 가 적절한 backend 로 분기.
 *
 * <p>Task 별 권장 모델 (룰 90 §10 — 하이브리드 + 비용 최적화):
 *
 * <ul>
 *   <li><b>HIGH_QUALITY task</b> ({@code PERSONA_INSIGHT}, {@code STRATEGY}, {@code VISION}, {@code AUDIO_TRANSCRIBE})
 *       → Gemini 2.5 Flash (한국어 1티어 + Vision/Audio + 1000 RPD 무료)
 *   <li><b>FAST task</b> ({@code SENTIMENT}, {@code KEYWORD_EXTRACT}, {@code TRANSLATE}, {@code NORMALIZE})
 *       → Groq Llama 3.3 70B (수백 tps + 14400 RPD 무료)
 * </ul>
 */
public enum LlmTask {
    /** 4 페르소나 분기 인사이트 — 한국어 톤 + 추론 품질 우선 (Gemini) */
    PERSONA_INSIGHT(true),

    /** Evidence/Insight/Strategy 3-step 의 Strategy 추천 (Gemini) */
    STRATEGY(true),

    /** D3 그래픽 성향 — image understanding (Gemini Vision) */
    VISION(true),

    /** P7 인터뷰 STT — audio 30s (Gemini Audio) */
    AUDIO_TRANSCRIBE(true),

    /** D5 sentiment 분류 (POS/NEU/NEG) — 빠른 분류 (Groq) */
    SENTIMENT(false),

    /** mention 키워드 추출 — 빠른 task (Groq) */
    KEYWORD_EXTRACT(false),

    /** 영어→한국어 번역 — 짧은 텍스트 (Groq) */
    TRANSLATE(false),

    /** 게임명/메타 정규화 — 데이터 후처리 (Groq) */
    NORMALIZE(false);

    private final boolean highQuality;

    LlmTask(boolean highQuality) {
        this.highQuality = highQuality;
    }

    /** true = Gemini (품질 우선) / false = Groq (속도/비용 우선) */
    public boolean isHighQuality() {
        return highQuality;
    }
}
