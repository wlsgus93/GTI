-- V3: 프로토타입용 샘플 게임 + 시계열 스냅샷 시드.
-- 실제 운영에서는 일일 수집 잡(DailyIngestionService)이 자동 채우지만,
-- 프론트 P1 트렌드 보드를 즉시 표시하기 위한 시드.

-- 10개 인기 게임 (Steam appid 기준)
INSERT INTO games (steam_app_id, name, developer, publisher) VALUES
    (730,     'Counter-Strike 2',  'Valve',                'Valve'),
    (570,     'Dota 2',            'Valve',                'Valve'),
    (1245620, 'ELDEN RING',        'FromSoftware',         'Bandai Namco'),
    (413150,  'Stardew Valley',    'ConcernedApe',         'ConcernedApe'),
    (1091500, 'Cyberpunk 2077',    'CD Projekt RED',       'CD Projekt'),
    (1145360, 'Hades',             'Supergiant Games',     'Supergiant Games'),
    (367520,  'Hollow Knight',     'Team Cherry',          'Team Cherry'),
    (646570,  'Slay the Spire',    'Mega Crit',            'Mega Crit'),
    (588650,  'Dead Cells',        'Motion Twin',          'Motion Twin'),
    (440,     'Team Fortress 2',   'Valve',                'Valve');

-- 어제(t-1d) + 오늘(t-1h) 두 시점 CCU 스냅샷 → ccuDeltaPct 계산 가능.
-- 값은 실제 Steam Charts에 가까운 근사치 (프로토타입).

-- 어제: 24시간 전
INSERT INTO player_snapshot (game_id, concurrent_players, captured_at, source, stale)
SELECT g.id, vals.ccu, now() - interval '24 hours', 'STEAM', false
FROM games g JOIN (VALUES
    (730,     1050000),
    (570,      720000),
    (1245620,  185000),
    (413150,    78000),
    (1091500,   48000),
    (1145360,    4900),
    (367520,     8200),
    (646570,     3950),
    (588650,     2900),
    (440,      102000)
) AS vals(steam_app_id, ccu) ON g.steam_app_id = vals.steam_app_id;

-- 현재(최근): 1시간 전
INSERT INTO player_snapshot (game_id, concurrent_players, captured_at, source, stale)
SELECT g.id, vals.ccu, now() - interval '1 hour', 'STEAM', false
FROM games g JOIN (VALUES
    (730,     1100000),  -- +4.7%
    (570,      700000),  -- -2.8%
    (1245620,  200000),  -- +8.1%
    (413150,    80000),  -- +2.6%
    (1091500,   50000),  -- +4.2%
    (1145360,    5100),  -- +4.1%
    (367520,     8000),  -- -2.4%
    (646570,     4100),  -- +3.8%
    (588650,     3000),  -- +3.4%
    (440,      100000)   -- -2.0%
) AS vals(steam_app_id, ccu) ON g.steam_app_id = vals.steam_app_id;

-- 인덱스 통계 갱신 (PostgreSQL)
ANALYZE games;
ANALYZE player_snapshot;
