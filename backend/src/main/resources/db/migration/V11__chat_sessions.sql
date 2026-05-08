-- W8 D4: 3-Layer 하이브리드 (로컬 분류기 + 클라우드 LLM) 의 채팅 세션/이력
-- 꼬리질문 컨텍스트 유지 + 비용 추적 (token 누적 + cache 비율)
--
-- 차별점 (룰 95):
--   3-Layer 하이브리드 = 로컬 분류 (LLM 호출 X) → DB 컨텍스트 → cloud LLM
--   일 100 query 가정: 30% 차단 + 캐시 hit 50% = 실 호출 35건 → 토큰 65% 절감

-- chat_session: 사용자 채팅 세션 (1 user x N session, 세션당 한 페르소나 고정)
CREATE TABLE chat_session (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    persona         VARCHAR(20) NOT NULL,
    title           VARCHAR(200),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    total_messages  INT NOT NULL DEFAULT 0,
    total_tokens    INT NOT NULL DEFAULT 0,
    closed_at       TIMESTAMPTZ
);

CREATE INDEX idx_chat_session_user_active
    ON chat_session(user_id, last_active_at DESC) WHERE closed_at IS NULL;

-- chat_message: 메시지 이력 + 분류 결과 + 토큰 사용량
CREATE TABLE chat_message (
    id                    BIGSERIAL PRIMARY KEY,
    session_id            BIGINT NOT NULL REFERENCES chat_session(id) ON DELETE CASCADE,
    role                  VARCHAR(20) NOT NULL,    -- 'user' / 'assistant' / 'system' / 'summary'
    content               TEXT NOT NULL,
    -- Layer 1 분류 결과 (user message 만)
    classified_topic      VARCHAR(20),             -- GAME / OFF_TOPIC / SMALL_TALK
    classified_intent     VARCHAR(30),             -- NEW_QUERY / FOLLOW_UP / PERSONA_SWITCH / META / UNCLEAR
    classified_confidence DOUBLE PRECISION,
    classifier_blocked    BOOLEAN NOT NULL DEFAULT FALSE,  -- true = cloud 호출 X (비용 추적)
    -- Layer 3 cloud LLM 호출 메타 (assistant message 만)
    referenced_game_ids   BIGINT[],                -- FOLLOW_UP 추적용
    prompt_tokens         INT,
    completion_tokens     INT,
    model                 VARCHAR(50),             -- 사용된 모델 (gemini-2.5-flash 등)
    cached                BOOLEAN NOT NULL DEFAULT FALSE,
    latency_ms            INT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_message_session_created
    ON chat_message(session_id, created_at DESC);

CREATE INDEX idx_chat_message_classifier_blocked
    ON chat_message(classifier_blocked) WHERE classifier_blocked = TRUE;

-- 비용 추적 view (룰 95 정량 메트릭)
CREATE VIEW v_chat_cost_summary AS
SELECT
    DATE_TRUNC('day', created_at) AS day,
    role,
    COUNT(*) AS message_count,
    SUM(CASE WHEN classifier_blocked THEN 1 ELSE 0 END) AS blocked_count,
    SUM(CASE WHEN cached THEN 1 ELSE 0 END) AS cached_count,
    SUM(COALESCE(prompt_tokens, 0)) AS total_prompt_tokens,
    SUM(COALESCE(completion_tokens, 0)) AS total_completion_tokens,
    AVG(COALESCE(latency_ms, 0)) AS avg_latency_ms
FROM chat_message
GROUP BY DATE_TRUNC('day', created_at), role;

COMMENT ON TABLE chat_session IS 'W8 D4: 3-Layer 하이브리드 채팅 세션 — 페르소나 세션당 고정';
COMMENT ON TABLE chat_message IS 'W8 D4: 메시지 이력 + Layer 1 분류 + Layer 3 토큰 사용량 추적';
COMMENT ON VIEW v_chat_cost_summary IS '일별 비용 추적 — blocked/cached 비율 + token 누적';
