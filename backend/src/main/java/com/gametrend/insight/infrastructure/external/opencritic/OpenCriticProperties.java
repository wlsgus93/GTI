package com.gametrend.insight.infrastructure.external.opencritic;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gti.external.opencritic")
public record OpenCriticProperties(String baseUrl, String apiKey) {}
