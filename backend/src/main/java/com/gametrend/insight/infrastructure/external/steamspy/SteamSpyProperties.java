package com.gametrend.insight.infrastructure.external.steamspy;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gti.external.steamspy")
public record SteamSpyProperties(String baseUrl) {}
