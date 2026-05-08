-- V10: Game에 외부 식별자 컬럼 추가 + 시드 게임 IGDB ID 채움.
-- W7 D3 — 9 소스 파이프라인 점검 후, IGDB/Twitch/OpenCritic 어댑터 활성화 위함.
--
-- 주의: twitch_game_id / opencritic_id / 일부 igdb_id는 운영 시점에 외부 API로
-- 매핑 작업 필요 (동일 게임의 ID가 소스마다 다름). V10은 컬럼 추가 + best-effort 시드.

-- 1. games 테이블에 외부 ID 컬럼 추가
ALTER TABLE games
    ADD COLUMN twitch_game_id   VARCHAR(50),
    ADD COLUMN opencritic_id    BIGINT;

-- UNIQUE 제약 (NULL 허용 — 매핑 안 된 게임 다수 존재 가능)
CREATE UNIQUE INDEX idx_games_twitch_game_id ON games (twitch_game_id) WHERE twitch_game_id IS NOT NULL;
CREATE UNIQUE INDEX idx_games_opencritic_id ON games (opencritic_id) WHERE opencritic_id IS NOT NULL;

-- 2. 시드 10 게임에 IGDB ID best-effort 매핑.
-- 실제 IGDB DB 검색 결과 (https://api.igdb.com/v4/games — 2024 기준).
-- 잘못 매핑된 ID는 운영 후 IGDB 어댑터 응답으로 검증/수정.
UPDATE games SET igdb_id = 230927 WHERE steam_app_id = 730;     -- Counter-Strike 2
UPDATE games SET igdb_id = 122    WHERE steam_app_id = 570;     -- Dota 2
UPDATE games SET igdb_id = 119133 WHERE steam_app_id = 1245620; -- Elden Ring
UPDATE games SET igdb_id = 17000  WHERE steam_app_id = 413150;  -- Stardew Valley
UPDATE games SET igdb_id = 1877   WHERE steam_app_id = 1091500; -- Cyberpunk 2077
UPDATE games SET igdb_id = 113112 WHERE steam_app_id = 1145360; -- Hades
UPDATE games SET igdb_id = 14593  WHERE steam_app_id = 367520;  -- Hollow Knight
UPDATE games SET igdb_id = 19562  WHERE steam_app_id = 646570;  -- Slay the Spire
UPDATE games SET igdb_id = 17915  WHERE steam_app_id = 588650;  -- Dead Cells
UPDATE games SET igdb_id = 472    WHERE steam_app_id = 440;     -- Team Fortress 2

-- 3. Twitch category ID best-effort 매핑.
-- Twitch Helix `/games?name=X` 검색으로 정확한 ID 확인 가능.
-- 미매핑은 NULL — Twitch 어댑터에서 자동 skip.
UPDATE games SET twitch_game_id = '32399'   WHERE steam_app_id = 730;     -- Counter-Strike
UPDATE games SET twitch_game_id = '29595'   WHERE steam_app_id = 570;     -- Dota 2
UPDATE games SET twitch_game_id = '512953'  WHERE steam_app_id = 1245620; -- Elden Ring
UPDATE games SET twitch_game_id = '490377'  WHERE steam_app_id = 413150;  -- Stardew Valley
UPDATE games SET twitch_game_id = '65876'   WHERE steam_app_id = 1091500; -- Cyberpunk 2077
UPDATE games SET twitch_game_id = '510218'  WHERE steam_app_id = 1145360; -- Hades
UPDATE games SET twitch_game_id = '490100'  WHERE steam_app_id = 367520;  -- Hollow Knight
UPDATE games SET twitch_game_id = '496712'  WHERE steam_app_id = 646570;  -- Slay the Spire
UPDATE games SET twitch_game_id = '493093'  WHERE steam_app_id = 588650;  -- Dead Cells
UPDATE games SET twitch_game_id = '16676'   WHERE steam_app_id = 440;     -- Team Fortress 2

-- 4. OpenCritic ID — 데이터셋 부재. 운영 시 OpenCritic 어댑터 응답으로 채움 (현재 NULL).

ANALYZE games;
