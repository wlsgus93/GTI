package com.gametrend.insight.application.game;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * P2 게임 상세 — 개요 탭 응답.
 *
 * <p>프론트 `GameDetailPage` (개요 탭)이 표시할 정보 + 헤더 영역의 최신 CCU/변화율.
 *
 * @param id              GTI DB PK
 * @param steamAppId      Steam appid (nullable)
 * @param igdbId          IGDB id (nullable)
 * @param name            게임 명
 * @param description     설명 (Markdown 가능, nullable)
 * @param developer       개발사
 * @param publisher       퍼블리셔
 * @param releaseDate     출시일
 * @param coverImageUrl   커버 이미지 URL
 * @param genres          장르 명 리스트 (W2 Day 1엔 비어있을 수 있음)
 * @param latestCcu       최근 CCU (없으면 null)
 * @param ccuDeltaPct     24h CCU 변화율 (없으면 null)
 * @param createdAt       DB 생성 시각
 * @param updatedAt       DB 수정 시각
 */
public record GameDetailItem(
        Long id,
        Long steamAppId,
        Long igdbId,
        String name,
        String description,
        String developer,
        String publisher,
        LocalDate releaseDate,
        String coverImageUrl,
        List<String> genres,
        Integer latestCcu,
        Double ccuDeltaPct,
        Instant createdAt,
        Instant updatedAt) {
}
