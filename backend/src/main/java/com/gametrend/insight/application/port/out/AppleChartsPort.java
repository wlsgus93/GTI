package com.gametrend.insight.application.port.out;

import java.util.List;
import java.util.Optional;

/**
 * Apple Top Charts 포트 — 카테고리별 상위 앱 목록 조회.
 *
 * <p>per-game 호출이 아니라 **카테고리/국가별 일괄 호출** (예: "us 무료 게임 Top 100").
 * 결과를 게임 마스터 발견(discovery)에 사용. 1시간 캐시.
 */
public interface AppleChartsPort {

    /**
     * 특정 국가의 상위 무료 게임 목록을 반환.
     *
     * @param country 2-letter 국가 코드 (예: "us", "kr")
     * @param limit   최대 항목 수 (10, 25, 50, 100 중 하나 권장)
     */
    Optional<List<TopAppEntry>> fetchTopFreeGames(String country, int limit);

    record TopAppEntry(
            String id,
            String name,
            String artistName,
            int rank // 1부터 시작
            ) {}
}
