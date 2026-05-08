package com.gametrend.insight.infrastructure.external.igdb;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gti.external.igdb")
public record IgdbProperties(String baseUrl) {}
