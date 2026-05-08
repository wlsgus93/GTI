-- V4: PlayerSnapshot에 SteamSpy owners 범위 컬럼 추가 + 10 시드 게임에 가격/owners 시드.
-- W2 Day 4 Economics 모듈 입력 데이터.

ALTER TABLE player_snapshot
    ADD COLUMN owners_low  BIGINT NULL,
    ADD COLUMN owners_high BIGINT NULL,
    ADD CONSTRAINT chk_owners_range CHECK (
        (owners_low IS NULL AND owners_high IS NULL)
        OR (owners_low IS NOT NULL AND owners_high IS NOT NULL AND owners_low <= owners_high)
    );

-- SteamSpy 'STEAM_SPY' 출처로 owners 범위 시드 (현재 시점, 1h ago).
-- 값은 공개 정보 기반 합리적 추정 — 실 SteamSpy ingestion이 갱신할 placeholder.
INSERT INTO player_snapshot (game_id, owners_low, owners_high, captured_at, source, stale)
SELECT g.id, vals.lo, vals.hi, now() - interval '1 hour', 'STEAM_SPY', false
FROM games g JOIN (VALUES
    (730,     50000000::bigint,  100000000::bigint),  -- CS2 (F2P, 매우 높음)
    (570,     100000000::bigint, 200000000::bigint),  -- Dota 2 (F2P)
    (1245620, 20000000::bigint,   30000000::bigint),  -- Elden Ring
    (413150,  15000000::bigint,   25000000::bigint),  -- Stardew
    (1091500, 25000000::bigint,   35000000::bigint),  -- Cyberpunk
    (1145360,  5000000::bigint,    7000000::bigint),  -- Hades
    (367520,  10000000::bigint,   15000000::bigint),  -- Hollow Knight
    (646570,   4000000::bigint,    6000000::bigint),  -- Slay the Spire
    (588650,   5000000::bigint,    8000000::bigint),  -- Dead Cells
    (440,     50000000::bigint,  100000000::bigint)   -- TF2 (F2P 전환됨)
) AS vals(steam_app_id, lo, hi) ON g.steam_app_id = vals.steam_app_id;

-- 가격 시드 (Steam Storefront, USD).
-- F2P 게임은 0, 유료 게임은 정가 (USD cents).
INSERT INTO price_snapshot (game_id, currency, price_cents, discount_percent, captured_at, source, stale)
SELECT g.id, 'USD', vals.cents, 0, now() - interval '1 hour', 'STEAM_STORE', false
FROM games g JOIN (VALUES
    (730,        0::bigint),  -- F2P
    (570,        0::bigint),  -- F2P
    (1245620, 5999::bigint),  -- $59.99
    (413150,  1499::bigint),  -- $14.99
    (1091500, 5999::bigint),  -- $59.99
    (1145360, 2499::bigint),  -- $24.99
    (367520,  1499::bigint),  -- $14.99
    (646570,  2499::bigint),  -- $24.99
    (588650,  2499::bigint),  -- $24.99
    (440,        0::bigint)   -- F2P 전환
) AS vals(steam_app_id, cents) ON g.steam_app_id = vals.steam_app_id;

ANALYZE player_snapshot;
ANALYZE price_snapshot;
