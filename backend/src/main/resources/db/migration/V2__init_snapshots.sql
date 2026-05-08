-- V2: Time-series snapshot tables.
-- 9개 외부 소스에서 수집한 데이터를 시점별로 저장.
-- 인덱스 (game_id, captured_at DESC)로 최신 N개 조회 최적화.

-- 가격 시계열
CREATE TABLE price_snapshot (
    id                  BIGSERIAL PRIMARY KEY,
    game_id             BIGINT      NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    currency            VARCHAR(3)  NOT NULL,                 -- "USD", "KRW", ...
    price_cents         BIGINT      NOT NULL,                 -- 부동소수 회피
    discount_percent    INTEGER     NOT NULL DEFAULT 0
        CHECK (discount_percent BETWEEN 0 AND 100),
    captured_at         TIMESTAMPTZ NOT NULL,
    source              VARCHAR(20) NOT NULL,                 -- STEAM, STEAM_STORE, ...
    stale               BOOLEAN     NOT NULL DEFAULT false    -- 캐시 fallback 여부
);
CREATE INDEX idx_price_snapshot_game_time ON price_snapshot (game_id, captured_at DESC);

-- 플레이어 / 리뷰 시계열
CREATE TABLE player_snapshot (
    id                      BIGSERIAL PRIMARY KEY,
    game_id                 BIGINT      NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    concurrent_players      INTEGER,                          -- Steam CCU
    review_score_positive   INTEGER,                          -- positive review count
    review_score_total      INTEGER,                          -- total review count
    captured_at             TIMESTAMPTZ NOT NULL,
    source                  VARCHAR(20) NOT NULL,
    stale                   BOOLEAN     NOT NULL DEFAULT false
);
CREATE INDEX idx_player_snapshot_game_time ON player_snapshot (game_id, captured_at DESC);

-- 시청자 시계열 (Twitch / YouTube)
CREATE TABLE viewer_snapshot (
    id                  BIGSERIAL PRIMARY KEY,
    game_id             BIGINT      NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    concurrent_viewers  INTEGER     NOT NULL,
    captured_at         TIMESTAMPTZ NOT NULL,
    source              VARCHAR(20) NOT NULL,
    stale               BOOLEAN     NOT NULL DEFAULT false
);
CREATE INDEX idx_viewer_snapshot_game_time ON viewer_snapshot (game_id, captured_at DESC);

-- 커뮤니티 멘션 시계열 (Reddit / etc)
CREATE TABLE mention_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT      NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    mention_count   INTEGER     NOT NULL,
    sentiment       VARCHAR(10),                              -- POS / NEU / NEG / NULL
    captured_at     TIMESTAMPTZ NOT NULL,
    source          VARCHAR(20) NOT NULL,
    stale           BOOLEAN     NOT NULL DEFAULT false
);
CREATE INDEX idx_mention_snapshot_game_time ON mention_snapshot (game_id, captured_at DESC);
