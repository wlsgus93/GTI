package com.gametrend.insight.presentation.admin;

import com.gametrend.insight.application.ingestion.DailyIngestionService;
import com.gametrend.insight.application.ingestion.DailyIngestionService.AppleChartsSummary;
import com.gametrend.insight.application.ingestion.DailyIngestionService.IngestionRunSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 운영용 — 일일 수집 잡 수동 trigger.
 *
 * <p>스케줄러는 매일 03:00 UTC. 즉시 실행이 필요한 경우 (개발 / smoke / 데모) 이 endpoint 사용.
 *
 * <p>인증: 현재 SecurityConfig에 `/api/v1/admin/**` 별도 정책 없음 → permitAll.
 * 운영 배포 전 admin role 가드 필요 (W7+ 후속).
 */
@RestController
@RequestMapping("/api/v1/admin/ingestion")
@Tag(name = "Admin — Ingestion", description = "9 소스 수집 잡 수동 trigger (개발/smoke/데모용)")
public class IngestionAdminController {

    private final DailyIngestionService ingestionService;

    public IngestionAdminController(DailyIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/run")
    @Operation(
            summary = "수집 잡 즉시 실행",
            description =
                    "등록된 모든 게임 대상으로 8 per-game 어댑터 (Steam Web/Storefront/Spy, "
                            + "Twitch, IGDB, YouTube, Reddit, OpenCritic) 병렬 호출. "
                            + "Apple RSS는 카테고리 호출이라 별도 endpoint (`GET /apple-charts`). "
                            + "각 게임의 매핑된 ID가 null인 어댑터는 skip. "
                            + "응답: 게임 수 / 소스 success/failure/empty/persisted 카운트 / wall-clock ms.")
    public IngestionRunSummary runOnce() {
        return ingestionService.runOnce();
    }

    @GetMapping("/apple-charts")
    @Operation(
            summary = "Apple Top Charts 카테고리 호출 (smoke / discovery)",
            description =
                    "per-game이 아닌 카테고리/국가별 일괄 호출. 게임 마스터 발견(discovery) 용도. "
                            + "현재는 응답 검증만 — 새 게임 자동 등록은 후속.")
    public AppleChartsSummary appleCharts(
            @RequestParam(defaultValue = "us") String country,
            @RequestParam(defaultValue = "10") int limit) {
        return ingestionService.fetchAppleTopFree(country, limit);
    }
}
