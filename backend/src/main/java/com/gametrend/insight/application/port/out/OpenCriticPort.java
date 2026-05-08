package com.gametrend.insight.application.port.out;

import java.util.Optional;

/**
 * OpenCritic 평론 점수 포트.
 *
 * <p>OpenCritic의 게임 ID는 별도 (Steam appid와 다름). Game 마스터에 매핑 저장 필요 (Day 7+).
 * 응답은 평론 평균 점수 + 티어 정보.
 */
public interface OpenCriticPort {

    /**
     * OpenCritic 게임 ID로 점수/메타 조회.
     *
     * @param gameId          GTI DB 게임 PK
     * @param openCriticGameId OpenCritic 내부 ID
     */
    Optional<OpenCriticScore> fetchScore(long gameId, long openCriticGameId);

    record OpenCriticScore(
            long openCriticGameId,
            String name,
            Double topCriticScore,
            String tier,
            Integer reviewCount) {}
}
