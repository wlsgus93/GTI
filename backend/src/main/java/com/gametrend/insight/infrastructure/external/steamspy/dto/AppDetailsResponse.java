package com.gametrend.insight.infrastructure.external.steamspy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * SteamSpy {@code /api.php?request=appdetails&appid=...} 응답 (필요 필드만).
 *
 * <p>응답 예: {@code {"appid":730,"name":"CS2","owners":"50,000,000 .. 100,000,000",
 * "average_forever":5000,"median_2weeks":120,"ccu":1500000}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppDetailsResponse(
        @JsonProperty("appid") long appId,
        String name,
        String owners,
        @JsonProperty("average_forever") Integer averageForever,
        @JsonProperty("median_2weeks") Integer median2weeks,
        Integer ccu) {}
