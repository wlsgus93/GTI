-- V9: mention_snapshot 시드 (W6 D3 — D5 커뮤니티 활성도).
-- 10 게임 × 2 source (YOUTUBE/REDDIT) × 3 sentiment (POS/NEU/NEG) = 60 rows.

-- mention_count 분포 (인기 게임 ↑ / 인디 게임 ↓):
INSERT INTO mention_snapshot (game_id, mention_count, sentiment, captured_at, source, stale)
SELECT g.id, vals.cnt, vals.sent, now() - interval '1 hour', vals.src, false
FROM games g JOIN (VALUES
    -- CS2 (730) — 매우 활발한 커뮤니티
    (730, 'YOUTUBE', 'POS', 8500),
    (730, 'YOUTUBE', 'NEU', 4200),
    (730, 'YOUTUBE', 'NEG', 1800),
    (730, 'REDDIT',  'POS', 5200),
    (730, 'REDDIT',  'NEU', 3100),
    (730, 'REDDIT',  'NEG', 2400),
    -- Dota 2 (570) — 활발하지만 NEG 비율 약간 높음 (matchmaking 불만 등)
    (570, 'YOUTUBE', 'POS', 4500),
    (570, 'YOUTUBE', 'NEU', 2800),
    (570, 'YOUTUBE', 'NEG', 1900),
    (570, 'REDDIT',  'POS', 3200),
    (570, 'REDDIT',  'NEU', 2200),
    (570, 'REDDIT',  'NEG', 2100),
    -- Elden Ring (1245620) — POS 압도적 (호평 큰 게임)
    (1245620, 'YOUTUBE', 'POS', 6800),
    (1245620, 'YOUTUBE', 'NEU', 1400),
    (1245620, 'YOUTUBE', 'NEG', 320),
    (1245620, 'REDDIT',  'POS', 4100),
    (1245620, 'REDDIT',  'NEU', 980),
    (1245620, 'REDDIT',  'NEG', 280),
    -- Stardew Valley (413150) — 인디지만 충성 팬덤
    (413150, 'YOUTUBE', 'POS', 1850),
    (413150, 'YOUTUBE', 'NEU', 420),
    (413150, 'YOUTUBE', 'NEG', 95),
    (413150, 'REDDIT',  'POS', 1620),
    (413150, 'REDDIT',  'NEU', 380),
    (413150, 'REDDIT',  'NEG', 110),
    -- Cyberpunk 2077 (1091500) — NEG 높은 게임 (출시 이슈 잔존)
    (1091500, 'YOUTUBE', 'POS', 2200),
    (1091500, 'YOUTUBE', 'NEU', 1850),
    (1091500, 'YOUTUBE', 'NEG', 2700),
    (1091500, 'REDDIT',  'POS', 1450),
    (1091500, 'REDDIT',  'NEU', 1320),
    (1091500, 'REDDIT',  'NEG', 1980),
    -- Hades (1145360) — 인디 + POS 강함
    (1145360, 'YOUTUBE', 'POS', 1240),
    (1145360, 'YOUTUBE', 'NEU', 280),
    (1145360, 'YOUTUBE', 'NEG', 65),
    (1145360, 'REDDIT',  'POS', 980),
    (1145360, 'REDDIT',  'NEU', 220),
    (1145360, 'REDDIT',  'NEG', 88),
    -- Hollow Knight (367520) — 인디
    (367520, 'YOUTUBE', 'POS', 980),
    (367520, 'YOUTUBE', 'NEU', 240),
    (367520, 'YOUTUBE', 'NEG', 78),
    (367520, 'REDDIT',  'POS', 1100),
    (367520, 'REDDIT',  'NEU', 280),
    (367520, 'REDDIT',  'NEG', 92),
    -- Slay the Spire (646570) — 인디
    (646570, 'YOUTUBE', 'POS', 720),
    (646570, 'YOUTUBE', 'NEU', 180),
    (646570, 'YOUTUBE', 'NEG', 55),
    (646570, 'REDDIT',  'POS', 850),
    (646570, 'REDDIT',  'NEU', 210),
    (646570, 'REDDIT',  'NEG', 68),
    -- Dead Cells (588650) — 인디
    (588650, 'YOUTUBE', 'POS', 580),
    (588650, 'YOUTUBE', 'NEU', 145),
    (588650, 'YOUTUBE', 'NEG', 48),
    (588650, 'REDDIT',  'POS', 690),
    (588650, 'REDDIT',  'NEU', 175),
    (588650, 'REDDIT',  'NEG', 62),
    -- TF2 (440) — 오래된 게임, 스토리 분포
    (440, 'YOUTUBE', 'POS', 1450),
    (440, 'YOUTUBE', 'NEU', 880),
    (440, 'YOUTUBE', 'NEG', 1280),
    (440, 'REDDIT',  'POS', 920),
    (440, 'REDDIT',  'NEU', 540),
    (440, 'REDDIT',  'NEG', 780)
) AS vals(steam_app_id, src, sent, cnt) ON g.steam_app_id = vals.steam_app_id;

ANALYZE mention_snapshot;
