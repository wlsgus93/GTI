package com.gametrend.insight.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Google Play Top Charts 포트 — 카테고리별 상위 앱 목록 조회.
 *
 * <p>Apple 과 달리 공식 RSS/API 가 없어 별도 마이크로서비스(crawler-service, Node.js + google-play-scraper)
 * 를 통해 조회. Spring 본체는 REST(JSON) 만 호출.
 *
 * <p>결과를 게임 마스터 발견(discovery)에 사용. 1시간 캐시.
 */
public interface GooglePlayChartsPort {

    /**
     * 특정 국가의 상위 무료 게임 목록을 반환.
     *
     * @param country 2-letter 국가 코드 (예: "us", "kr")
     * @param limit   최대 항목 수 (1~100)
     */
    Optional<List<TopAppEntry>> fetchTopFreeGames(String country, int limit);

    record TopAppEntry(
            String appId, // Google Play package id (예: "com.example.game")
            String title,
            String developer,
            Double score, // 4.5 / null
            int rank // 1부터 시작
            ) {}
}
