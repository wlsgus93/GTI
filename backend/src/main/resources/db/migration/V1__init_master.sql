-- V1: Master tables (User, Game, Genre, GameGenre)
-- GTI 도메인 마스터 데이터.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE genres (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE games (
    id                  BIGSERIAL PRIMARY KEY,
    steam_app_id        BIGINT       UNIQUE,
    igdb_id             BIGINT       UNIQUE,
    name                VARCHAR(500) NOT NULL,
    description         TEXT,
    release_date        DATE,
    developer           VARCHAR(255),
    publisher           VARCHAR(255),
    cover_image_url     VARCHAR(500),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_games_name ON games (name);
CREATE INDEX idx_games_release_date ON games (release_date DESC) WHERE release_date IS NOT NULL;

CREATE TABLE game_genres (
    game_id     BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    genre_id    BIGINT NOT NULL REFERENCES genres(id) ON DELETE CASCADE,
    PRIMARY KEY (game_id, genre_id)
);

CREATE INDEX idx_game_genres_genre ON game_genres (genre_id);
