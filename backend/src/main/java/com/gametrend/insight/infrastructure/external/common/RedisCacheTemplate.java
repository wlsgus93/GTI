package com.gametrend.insight.infrastructure.external.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 외부 API 응답 캐시. JSON 직렬화 + TTL.
 *
 * <p>키 컨벤션: {@code ext:<source>:<operation>:<args>} (예: {@code ext:steam:players:570}).
 *
 * <p>실패 시 cache miss로 처리 (예외 던지지 않음) — 캐시 장애가 비즈니스 흐름을 막지 않도록.
 */
@Component
public class RedisCacheTemplate {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheTemplate.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisCacheTemplate(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, type));
        } catch (JsonProcessingException e) {
            log.warn("Cache deserialize failed for key={}: {}", key, e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Cache get failed for key={}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    public <T> void put(String key, T value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            redis.opsForValue().set(key, json, ttl);
        } catch (JsonProcessingException e) {
            log.warn("Cache serialize failed for key={}: {}", key, e.getMessage());
        } catch (Exception e) {
            log.warn("Cache put failed for key={}: {}", key, e.getMessage());
        }
    }

    public void evict(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Cache evict failed for key={}: {}", key, e.getMessage());
        }
    }
}
