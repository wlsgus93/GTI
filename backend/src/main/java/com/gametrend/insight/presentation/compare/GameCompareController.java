package com.gametrend.insight.presentation.compare;

import com.gametrend.insight.application.compare.CompareResult;
import com.gametrend.insight.application.compare.GameCompareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * P3 게임 비교 REST.
 *
 * <p>예: {@code GET /api/v1/games/compare?ids=1,5,7}
 */
@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Game Compare", description = "P3 게임 비교 (W3 Day 4)")
public class GameCompareController {

    private final GameCompareService service;

    public GameCompareController(GameCompareService service) {
        this.service = service;
    }

    @GetMapping("/compare")
    @Operation(
            summary = "여러 게임 한번에 비교 — Virtual Threads로 병렬 호출",
            description = "2~5 game ID. 미존재는 missingGameIds에 graceful 표시. "
                    + "응답에 wallClockMs 포함 (서버 측 병렬 호출 latency).")
    public CompareResult compare(
            @Parameter(description = "비교할 게임 ID (콤마 구분, 2~5개)", required = true)
                    @RequestParam("ids") List<Long> ids) {
        return service.compare(ids);
    }
}
