package com.gametrend.insight.infrastructure.external.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 외부 API 호출 메트릭 수집.
 *
 * <p>메트릭 이름:
 * <ul>
 *   <li>{@code external.api.duration} (timer, tags: source, outcome)
 *   <li>{@code external.api.calls} (counter, tags: source, status)
 *   <li>{@code external.api.cache.hit} (counter, tags: source)
 *   <li>{@code external.api.cache.miss} (counter, tags: source)
 * </ul>
 */
@Component
public class ExternalApiMetrics {

    private final MeterRegistry registry;

    public ExternalApiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    public void recordDuration(String source, String outcome, Timer.Sample sample) {
        sample.stop(Timer.builder("external.api.duration")
                .tag("source", source)
                .tag("outcome", outcome)
                .register(registry));
    }

    public void incrementCalls(String source, int status) {
        Counter.builder("external.api.calls")
                .tag("source", source)
                .tag("status", String.valueOf(status))
                .register(registry)
                .increment();
    }

    public void incrementCacheHit(String source) {
        Counter.builder("external.api.cache.hit").tag("source", source).register(registry).increment();
    }

    public void incrementCacheMiss(String source) {
        Counter.builder("external.api.cache.miss").tag("source", source).register(registry).increment();
    }
}
