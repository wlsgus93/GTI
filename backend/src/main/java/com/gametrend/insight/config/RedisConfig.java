package com.gametrend.insight.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring Boot가 기본으로 RedisTemplate을 자동 구성하지만, 명시적으로 String 직렬화를
 * 사용하는 StringRedisTemplate을 빈으로 노출한다.
 *
 * <p>실제 직렬화 (JSON ↔ Object)는 {@link com.gametrend.insight.infrastructure.external.common.RedisCacheTemplate}에서.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}
