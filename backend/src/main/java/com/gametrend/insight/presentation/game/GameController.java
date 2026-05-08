package com.gametrend.insight.presentation.game;

import com.gametrend.insight.application.game.CcuRange;
import com.gametrend.insight.application.game.CcuSeries;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.GameQueryService;
import com.gametrend.insight.application.game.PlayerInsight;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * P2 게임 상세 REST. 6 탭 중:
 * <ul>
 *   <li>W2 Day 2: 개요, CCU 시계열
 *   <li>W2 Day 3: 플레이어 분석 (CCU + 리뷰 + 시청자 + 멘션)
 *   <li>나머지: AI 인사이트 (Spring AI 활성화 후), 매출 (Money Calc), 단가 (CPV)
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Game Detail", description = "P2 게임 상세 (6 탭)")
public class GameController {

    private final GameQueryService gameQueryService;

    public GameController(GameQueryService gameQueryService) {
        this.gameQueryService = gameQueryService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "게임 상세 메타 + 최신 CCU/변화율", description = "P2 개요 탭 데이터")
    public GameDetailItem getDetail(@PathVariable long id) {
        return gameQueryService.getDetail(id);
    }

    @GetMapping("/{id}/ccu")
    @Operation(summary = "CCU 시계열", description = "P2 동접 탭 차트 데이터. range: 24h | 7d | 30d (기본) | 90d")
    public CcuSeries getCcuSeries(
            @PathVariable long id,
            @Parameter(description = "범위 코드 (24h | 7d | 30d | 90d)")
                    @RequestParam(name = "range", required = false) String range) {
        return gameQueryService.getCcuSeries(id, CcuRange.parse(range));
    }

    @GetMapping("/{id}/players")
    @Operation(
            summary = "플레이어 분석",
            description = "P2 플레이어 탭 — 최신 CCU + 리뷰 비율 + 트위치 시청자 + YouTube/Reddit 멘션 카운트")
    public PlayerInsight getPlayerInsight(@PathVariable long id) {
        return gameQueryService.getPlayerInsight(id);
    }
}
