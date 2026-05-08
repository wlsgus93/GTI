-- V8: P4 워치리스트 테이블 + JWT 인증 위한 user 시드.
-- W4 D4 — 사용자 시스템 첫 도입 (W1 임시 SecurityConfig permitAll → JWT 필터로 점진 교체).

CREATE TABLE watchlist_item (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    game_id     BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    note        VARCHAR(500),
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_watchlist_user_game UNIQUE (user_id, game_id)
);
CREATE INDEX idx_watchlist_user_added ON watchlist_item(user_id, added_at DESC);

-- 데모 사용자 1명 (개발 환경 — 운영 배포 전 삭제).
-- 비밀번호 'demo1234' BCrypt hash (cost=10).
INSERT INTO users (email, password_hash, display_name, role)
VALUES (
    'demo@gametrend.invalid',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Demo User',
    'USER'
) ON CONFLICT (email) DO NOTHING;
