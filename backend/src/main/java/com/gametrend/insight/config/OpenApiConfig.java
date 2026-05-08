package com.gametrend.insight.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gtiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GameTrend-Insight API")
                        .version("v1")
                        .description("검은토끼흰토끼 게임 시장 분석 플랫폼 — 9소스 인입, 7차원 분석, 에이전틱 UX")
                        .license(new License().name("Internal").url("https://gametrend.insight")));
    }
}
