package com.gametrend.insight.application.agent;

import com.gametrend.insight.domain.insight.Persona;

/**
 * 사용자 query 의 의도/주제 분류 (3-Layer 하이브리드의 Layer 1).
 *
 * <p><b>비용 절감 핵심</b>:
 * <ul>
 *   <li>로컬 작은 모델 (Llama 3.2 1B 등) 또는 정규식 빠른 분류 (~50 토큰, 1~3초)
 *   <li>OFF_TOPIC / SMALL_TALK → 클라우드 LLM 호출 차단 (토큰 0)
 *   <li>GAME → Gemini 등 cloud LLM 호출 (Layer 3)
 * </ul>
 *
 * <p><b>차별점 (3-Layer 하이브리드)</b>:
 * <ul>
 *   <li>일 100 query 가정: 30% 차단 + 70% 클라우드 → 캐시 hit 50% = <b>실 호출 35건</b>
 *   <li>모든 query 클라우드 대비 <b>토큰 65% 절감</b>
 *   <li>유료 환경 시: $30/월 → $10/월 (가정)
 * </ul>
 */
public interface IntentClassifier {

    Classification classify(String userQuery);

    /**
     * @param topic           대주제 (GAME / OFF_TOPIC / SMALL_TALK)
     * @param intent          세부 의도 (NEW_QUERY / FOLLOW_UP / PERSONA_SWITCH / META / UNCLEAR)
     * @param confidence      0.0~1.0
     * @param reason          분류 근거 (디버그)
     * @param inferredPersona 자동 추론된 페르소나 (W9 옵션 C — null = 추론 X / 명시 X)
     */
    record Classification(
            Topic topic,
            Intent intent,
            double confidence,
            String reason,
            Persona inferredPersona) {

        /** 페르소나 미추론 default 생성자 (구 callsite 호환). */
        public Classification(Topic topic, Intent intent, double confidence, String reason) {
            this(topic, intent, confidence, reason, null);
        }

        public boolean shouldCallCloud() {
            return topic == Topic.GAME && intent != Intent.UNCLEAR;
        }
    }

    enum Topic {
        /** 게임/게임 시장 분석 관련 query — 클라우드 LLM 호출 대상. */
        GAME,
        /** 게임 외 주제 — 거부 + "GTI 는 게임 시장 분석 도구" 안내. */
        OFF_TOPIC,
        /** 인사/사례/의성어 등 단순 잡담 — hardcoded 친근 응답. */
        SMALL_TALK
    }

    enum Intent {
        /** 새 분석 요청 — 컨텍스트 X, fresh start. */
        NEW_QUERY,
        /** 이전 응답에 대한 꼬리질문 — 채팅 세션 이력 컨텍스트 필요. */
        FOLLOW_UP,
        /** 페르소나/관점 변경 요청 — 같은 게임 다른 페르소나. */
        PERSONA_SWITCH,
        /** 메타 질문 — "이 도구는 뭐야", "어떻게 사용해" 등 도움말. */
        META,
        /** 분류 모호 — 사용자에게 재질문 권장. */
        UNCLEAR
    }
}
