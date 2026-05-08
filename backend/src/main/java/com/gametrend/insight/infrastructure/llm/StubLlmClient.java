package com.gametrend.insight.infrastructure.llm;

import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LLM 키 없는 환경 fallback. 운영 의도 X — 개발/테스트 부팅 가능성 확보.
 *
 * <p>응답에 STUB 표시를 명시해 클라이언트가 실 LLM 결과와 구분 가능.
 */
public final class StubLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(StubLlmClient.class);

    public static final String STUB_MODEL = "stub";

    @Override
    public LlmResponse complete(String systemPrompt, String userPrompt, int maxTokens) {
        log.warn("[STUB LLM] Anthropic API key 미설정 — 정적 응답 반환. 운영 환경에서는 ANTHROPIC_API_KEY 필수.");
        String body = """
                (개발 환경 — 실제 LLM 미연결)

                이 게임은 최근 데이터 기반 자동 요약을 제공할 수 없습니다.
                운영 환경에서는 ANTHROPIC_API_KEY 환경변수를 설정해 주세요.
                """;
        return new LlmResponse(body, 0, 0, STUB_MODEL);
    }
}
