package com.gametrend.insight.presentation.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 간단한 헬스/핑 엔드포인트 — Swagger UI 동작 검증용.
 * 실제 헬스체크는 Actuator의 /actuator/health 사용.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "Health check / ping")
public class HealthController {

    @GetMapping("/ping")
    @Operation(summary = "Ping", description = "단순 ping. 서버 동작 확인용.")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "gti",
                "status", "ok",
                "timestamp", Instant.now().toString());
    }
}
