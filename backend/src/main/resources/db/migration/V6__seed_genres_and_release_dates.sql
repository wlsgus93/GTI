-- V6: 장르 시드 + game_genres 매핑 + games.release_date 업데이트.
-- W3 D5 — D1 출시 동향 차원의 입력 데이터.

-- 장르 5종
INSERT INTO genres (name) VALUES
    ('Action'),
    ('RPG'),
    ('Indie'),
    ('Adventure'),
    ('Roguelike')
ON CONFLICT (name) DO NOTHING;

-- 게임별 출시일 + 장르 매핑 (실제 Steam 데이터 기준).
-- temp table로 (steam_app_id, release, genre) 조립 후 일괄 적용.

-- 출시일 업데이트
UPDATE games SET release_date = '2023-09-27' WHERE steam_app_id = 730;     -- CS2
UPDATE games SET release_date = '2013-07-09' WHERE steam_app_id = 570;     -- Dota 2
UPDATE games SET release_date = '2022-02-25' WHERE steam_app_id = 1245620; -- Elden Ring
UPDATE games SET release_date = '2016-02-26' WHERE steam_app_id = 413150;  -- Stardew Valley
UPDATE games SET release_date = '2020-12-10' WHERE steam_app_id = 1091500; -- Cyberpunk 2077
UPDATE games SET release_date = '2020-09-17' WHERE steam_app_id = 1145360; -- Hades
UPDATE games SET release_date = '2017-02-24' WHERE steam_app_id = 367520;  -- Hollow Knight
UPDATE games SET release_date = '2019-01-23' WHERE steam_app_id = 646570;  -- Slay the Spire
UPDATE games SET release_date = '2018-08-07' WHERE steam_app_id = 588650;  -- Dead Cells
UPDATE games SET release_date = '2007-10-10' WHERE steam_app_id = 440;     -- TF2

-- 장르 매핑 (게임당 1~2 장르)
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 730     AND gn.name = 'Action';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 570     AND gn.name = 'Action';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 1245620 AND gn.name = 'RPG';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 1245620 AND gn.name = 'Action';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 413150  AND gn.name = 'Indie';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 1091500 AND gn.name = 'RPG';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 1145360 AND gn.name = 'Indie';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 1145360 AND gn.name = 'Roguelike';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 367520  AND gn.name = 'Indie';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 367520  AND gn.name = 'Adventure';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 646570  AND gn.name = 'Indie';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 646570  AND gn.name = 'Roguelike';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 588650  AND gn.name = 'Indie';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 588650  AND gn.name = 'Roguelike';
INSERT INTO game_genres (game_id, genre_id)
SELECT g.id, gn.id FROM games g, genres gn WHERE g.steam_app_id = 440     AND gn.name = 'Action';

ANALYZE games;
ANALYZE genres;
ANALYZE game_genres;
