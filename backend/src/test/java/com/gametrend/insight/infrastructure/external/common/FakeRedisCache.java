package com.gametrend.insight.infrastructure.external.common;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 테스트용 in-memory 캐시. 실제 RedisCacheTemplate의 메서드를 오버라이드.
 *
 * <p>Day 3, 4의 inner FakeRedisCache를 통합 — Day 5부터 공유 사용.
 */
public class FakeRedisCache extends RedisCacheTemplate {

    private final Map<String, Object> store = new HashMap<>();

    public FakeRedisCache() {
        super(null, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object v = store.get(key);
        return v == null ? Optional.empty() : Optional.of((T) v);
    }

    @Override
    public <T> void put(String key, T value, Duration ttl) {
        store.put(key, value);
    }

    @Override
    public void evict(String key) {
        store.remove(key);
    }

    public int size() {
        return store.size();
    }

    public void clear() {
        store.clear();
    }
}
