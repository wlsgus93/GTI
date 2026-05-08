package com.gametrend.insight.presentation.trend;

import com.gametrend.insight.application.trend.TrendBoardItem;
import com.gametrend.insight.application.trend.TrendQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * P1 트렌드 보드 REST 엔드포인트.
 *
 * <p>현재는 단순 Top N 조회. W2 Day 2+에 차원 필터 (`?dim=hot|graphic|...`), 페이지네이션, 인사이트 코멘트 추가.
 */
@RestController
@RequestMapping("/api/v1/trends")
@Tag(name = "Trend Board", description = "P1 트렌드 보드 (시장 조망, TrendScore Top N)")
@Validated
public class TrendController {

    private final TrendQueryService trendQueryService;

    public TrendController(TrendQueryService trendQueryService) {
        this.trendQueryService = trendQueryService;
    }

    @GetMapping
    @Operation(summary = "TrendScore Top N 게임 카드", description = "P1 트렌드 보드 그리드 데이터")
    public TrendBoardResponse top(
            @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(200) int limit) {
        List<TrendBoardItem> items = trendQueryService.topByTrendScore(limit);
        return new TrendBoardResponse(items, items.size(), limit);
    }

    /** 단순 응답 envelope. 페이지네이션 메타는 W2 Day 2에 확장. */
    public record TrendBoardResponse(List<TrendBoardItem> content, int totalElements, int requestedLimit) {}
}
