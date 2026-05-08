package com.gametrend.insight.application.ingestion;

/**
 * 한 게임에 대한 수집 대상 식별자 묶음.
 *
 * <p>각 외부 소스마다 별도 ID 체계를 사용하므로 호출자(application 서비스)가 모두 알아서 전달.
 * Day 5에 IGDB id + gameName, Day 6에 OpenCritic id 추가.
 *
 * @param gameId            GTI DB 내 게임 PK
 * @param steamAppId        Steam appid (Steam Web + Storefront + SteamSpy에서 사용)
 * @param twitchGameId      Twitch 카테고리 ID
 * @param igdbId            IGDB 게임 ID
 * @param gameName          게임 이름 (YouTube/Reddit 검색 키워드)
 * @param openCriticGameId  OpenCritic 게임 ID
 */
public record IngestionTarget(
        Long gameId,
        Long steamAppId,
        String twitchGameId,
        Long igdbId,
        String gameName,
        Long openCriticGameId) {

    public IngestionTarget {
        if (gameId == null) {
            throw new IllegalArgumentException("gameId is required");
        }
        if (steamAppId == null
                && twitchGameId == null
                && igdbId == null
                && gameName == null
                && openCriticGameId == null) {
            throw new IllegalArgumentException("At least one source ID or gameName required");
        }
    }

    /** Day 4 호환 — Steam + Twitch만 지정. */
    public IngestionTarget(Long gameId, Long steamAppId, String twitchGameId) {
        this(gameId, steamAppId, twitchGameId, null, null, null);
    }

    /** Day 5 호환 — 5필드. */
    public IngestionTarget(Long gameId, Long steamAppId, String twitchGameId, Long igdbId, String gameName) {
        this(gameId, steamAppId, twitchGameId, igdbId, gameName, null);
    }
}
