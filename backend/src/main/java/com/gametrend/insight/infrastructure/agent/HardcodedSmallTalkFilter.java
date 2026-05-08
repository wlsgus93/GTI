package com.gametrend.insight.infrastructure.agent;

import java.util.Set;

/**
 * 정규식/키워드 기반 단순 분류 — LLM 호출 X.
 *
 * <p>가장 빠른 first-pass: 인사/사례/짧은 query 등 명확한 OFF_TOPIC/SMALL_TALK 차단.
 * 통과한 query 만 {@link com.gametrend.insight.application.agent.IntentClassifier} 의 LLM 분류로 전달.
 */
public final class HardcodedSmallTalkFilter {

    private static final Set<String> SMALL_TALK_TOKENS = Set.of(
            // 한국어 인사
            "안녕", "안녕하세요", "안녕히가세요", "잘가", "잘자", "좋은아침", "굿모닝", "굿나잇",
            // 사례
            "감사", "고마워", "고맙습니다", "땡큐", "thanks", "thank you",
            // 사과
            "미안", "죄송", "sorry",
            // 영어
            "hi", "hello", "hey", "good morning", "good night",
            // 의성어
            "ㅎㅎ", "ㅋㅋ", "ㅋ", "ㅎ", "ㅠㅠ", "ㅜㅜ", "ㅎㅎㅎ", "ㅋㅋㅋ",
            // 단순 테스트
            "test", "테스트", "ping", "핑");

    private static final Set<String> META_KEYWORDS = Set.of(
            "이 도구", "이 서비스", "GTI", "gti", "어떻게 사용", "how to use",
            "뭘 할 수", "뭘 해", "도움말", "help", "기능", "사용법");

    private HardcodedSmallTalkFilter() {}

    /** SMALL_TALK 인지 즉시 판정 (정확도 100% — 토큰 매칭). */
    public static boolean isSmallTalk(String query) {
        if (query == null) {
            return false;
        }
        String normalized = query.trim().toLowerCase();
        if (normalized.length() < 5) {
            return true; // 너무 짧음 — 의미 없는 query
        }
        return SMALL_TALK_TOKENS.contains(normalized);
    }

    /** META (도움말 요청) 인지 휴리스틱 판정. */
    public static boolean isMeta(String query) {
        if (query == null) {
            return false;
        }
        String normalized = query.trim().toLowerCase();
        for (String kw : META_KEYWORDS) {
            if (normalized.contains(kw.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /** SMALL_TALK 응답 — LLM 호출 없는 hardcoded 친근 응답. */
    public static String smallTalkResponse() {
        return "안녕하세요! 저는 GTI(GameTrend-Insight) 게임 시장 분석 에이전트입니다. "
                + "어떤 게임이나 장르의 시장 동향이 궁금하신가요?";
    }

    /** META 응답 — 사용 안내. */
    public static String metaResponse() {
        return "GTI는 게임 산업 종사자(인디 개발자/퍼블리셔/마케터/투자자)를 위한 시장 분석 도구입니다. "
                + "예시 질문: 'CS2 시장 분석해줘', '인디 RPG 출시 시기 추천', "
                + "'발로란트 vs 오버워치 비교'. 헤더의 페르소나 전환으로 관점을 바꿀 수 있습니다.";
    }

    /** OFF_TOPIC 응답 — 거부 + 정중한 안내. */
    public static String offTopicResponse() {
        return "이 질문은 게임 시장 분석 영역 밖입니다. GTI는 게임/게임 산업 관련 질문에 답변하도록 설계되었습니다. "
                + "예: 'CS2 동접자 추세는?', '인디 게임 마케팅 채널 추천'.";
    }
}
