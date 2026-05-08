-- V5: AI 분석/인사이트 영속화 테이블 (W2 Day 5).
-- LLM 호출 결과 + 토큰 사용량 추적 + TTL 기반 캐시 (24h 권장).

CREATE TABLE analysis (
    id                 BIGSERIAL PRIMARY KEY,
    game_id            BIGINT      NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    kind               VARCHAR(20) NOT NULL,        -- INSIGHT_BRIEF, ANOMALY, COMPETITIVE...
    prompt_version     VARCHAR(20) NOT NULL,        -- e.g. INSIGHT_V1
    content            TEXT        NOT NULL,        -- LLM 응답 본문
    prompt_tokens      INTEGER     NOT NULL DEFAULT 0,
    completion_tokens  INTEGER     NOT NULL DEFAULT 0,
    total_tokens       INTEGER     NOT NULL DEFAULT 0,
    model              VARCHAR(50),                 -- e.g. claude-opus-4-5, stub
    created_at         TIMESTAMPTZ NOT NULL,
    expires_at         TIMESTAMPTZ NOT NULL         -- TTL (createdAt + 24h 기본)
);

-- 최신 unexpired 조회 최적화: WHERE game_id=? AND kind=? AND expires_at > now() ORDER BY created_at DESC
CREATE INDEX idx_analysis_game_kind_expires ON analysis (game_id, kind, expires_at, created_at DESC);
