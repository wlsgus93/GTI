package com.gametrend.insight.domain.dimension.regional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * D7 — 지역·플랫폼 차원.
 *
 * <p>입력: Apple App Store + Google Play (모바일) — 멀티 국가 호출 결과.
 * 산출: Universal Hits (전 국가 인기) / Regional Hits (특정 국가만 인기) / Platform Divergent (플랫폼 편향).
 *
 * <p>알고리즘 (LLM 사용 X):
 * <ol>
 *   <li>국가 × 플랫폼 = N × 2 차트 병렬 호출 (Virtual Threads)
 *   <li>제목 정규화 (lowercase + alphanumeric) 으로 cross-country 매칭
 *   <li>Universal: 모든 국가 Top 20 안 진입한 게임
 *   <li>Regional: 한 국가만 Top 20, 다른 국가 미진입
 *   <li>Platform Divergent: Apple Top 진입 ↔ Google 미진입 (또는 반대)
 * </ol>
 */
public record RegionalPlatform(
        List<String> countries,
        List<Platform> platforms,
        Map<String, List<ChartEntry>> appleByCountry,
        Map<String, List<ChartEntry>> googleByCountry,
        List<UniversalHit> universalHits,
        Map<String, List<RegionalHit>> regionalHits,
        List<PlatformDivergent> platformDivergent,
        Summary summary,
        Instant capturedAt) {

    public enum Platform { APPLE, GOOGLE_PLAY }

    public record ChartEntry(
            String id,
            String title,
            String developer,
            int rank) {}

    /** 모든 (또는 대다수) 국가 Top N 진입 게임 — 글로벌 인기. */
    public record UniversalHit(
            String normalizedTitle,
            String displayTitle,
            Map<String, Integer> ranksByCountry, // country code → rank (Apple 우선)
            double avgRank,
            Set<Platform> platforms,
            int countriesCovered) {}

    /** 특정 국가만 Top N 진입 — 지역 편향. */
    public record RegionalHit(
            String normalizedTitle,
            String displayTitle,
            int rank,
            Platform platform) {}

    /** 한 플랫폼만 인기 (Apple Top 진입, Google 미진입 또는 반대). */
    public record PlatformDivergent(
            String normalizedTitle,
            String displayTitle,
            Platform onlyPlatform,
            String country,
            int rank) {}

    /** 요약 통계. */
    public record Summary(
            int totalCountries,
            int totalChartsLoaded, // = countries × platforms (성공 호출만)
            int universalHitCount,
            int regionalHitCount,
            int platformDivergentCount,
            long latencyMs) {}
}
