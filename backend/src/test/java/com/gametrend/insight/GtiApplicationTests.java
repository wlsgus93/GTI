package com.gametrend.insight;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 전체 컨텍스트 로딩 테스트.
 *
 * <p>현재 비활성화: Redis + Spring AI + Flyway가 모두 필요하여 단순 단위 테스트로 부적합.
 * W3에서 Testcontainers + ServiceConnection 기반 통합 테스트 베이스 구축 후 활성화.
 */
@SpringBootTest
@Disabled("W3에서 Testcontainers 통합 테스트 베이스 구축 후 활성화")
class GtiApplicationTests {

    @Test
    void contextLoads() {
        // 의도적 placeholder
    }
}
