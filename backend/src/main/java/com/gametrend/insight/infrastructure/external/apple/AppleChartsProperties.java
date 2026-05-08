package com.gametrend.insight.infrastructure.external.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gti.external.apple-charts")
public record AppleChartsProperties(String baseUrl) {}
