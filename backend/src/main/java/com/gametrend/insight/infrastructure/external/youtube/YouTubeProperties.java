package com.gametrend.insight.infrastructure.external.youtube;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gti.external.youtube")
public record YouTubeProperties(String baseUrl, String apiKey) {}
