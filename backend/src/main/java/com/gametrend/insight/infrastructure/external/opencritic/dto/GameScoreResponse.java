package com.gametrend.insight.infrastructure.external.opencritic.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OpenCritic {@code /api/game/{id}} 응답 (필요 필드만).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GameScoreResponse(
        long id,
        String name,
        @JsonProperty("topCriticScore") Double topCriticScore,
        @JsonProperty("tier") String tier,
        @JsonProperty("numReviews") Integer numReviews) {}
