package com.gametrend.insight.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Virtual Thread 명시적 Executor 빈.
 *
 * <p>{@code spring.threads.virtual.enabled=true}로 Tomcat 요청 처리는 자동 적용되지만,
 * 9소스 동시 수집 등 application 레이어에서 명시적으로 사용할 Executor를 빈으로 등록.
 *
 * <p>주의: Virtual Thread 안에서 {@code synchronized} + blocking I/O는 pinning을 유발한다.
 * {@code ReentrantLock} 또는 Redisson {@code RLock} 사용 권장.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean(name = "virtualThreadExecutor", destroyMethod = "shutdown")
    public ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
