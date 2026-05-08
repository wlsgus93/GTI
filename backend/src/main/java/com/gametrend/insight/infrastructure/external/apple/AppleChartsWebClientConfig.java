package com.gametrend.insight.infrastructure.external.apple;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Apple iTunes legacy RSS WebClient.
 *
 * <p>iTunes legacy RSS 는 응답을 {@code Content-Type: text/javascript;charset=UTF-8} 로 반환
 * (옛 JSONP 잔재). WebClient default 는 {@code application/json} 만 허용 → 직접 codec 등록.
 */
@Configuration
@EnableConfigurationProperties(AppleChartsProperties.class)
public class AppleChartsWebClientConfig {

    public static final String APPLE_CHARTS_WEB_CLIENT = "appleChartsWebClient";

    @Bean(APPLE_CHARTS_WEB_CLIENT)
    public WebClient appleChartsWebClient(
            WebClient.Builder builder, AppleChartsProperties props, ObjectMapper objectMapper) {
        // text/javascript 응답을 JSON 으로 디코드
        MediaType[] supportedTypes = new MediaType[] {
                MediaType.APPLICATION_JSON,
                MediaType.parseMediaType("text/javascript"),
                MediaType.parseMediaType("text/javascript;charset=UTF-8"),
                MediaType.parseMediaType("application/javascript")
        };
        Jackson2JsonDecoder decoder = new Jackson2JsonDecoder(objectMapper, supportedTypes);
        Jackson2JsonEncoder encoder = new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON);

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    ClientCodecConfigurer.ClientDefaultCodecs defaults = configurer.defaultCodecs();
                    defaults.jackson2JsonDecoder(decoder);
                    defaults.jackson2JsonEncoder(encoder);
                })
                .build();

        return builder.clone()
                .baseUrl(props.baseUrl())
                .exchangeStrategies(strategies)
                .build();
    }
}
