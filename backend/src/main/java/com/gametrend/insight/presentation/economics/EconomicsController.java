package com.gametrend.insight.presentation.economics;

import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.economics.EconomicsQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P2 게임 상세 — 매출/단가 탭 (W2 Day 4).
 *
 * <p>입력 데이터: SteamSpy owners + Steam 가격 + Twitch 시청자 + YT/Reddit mentions.
 * 광고비 부재로 직접 매출 X, 추정 매출 + 효율 단가 지표.
 */
@RestController
@RequestMapping("/api/v1/games")
@Tag(name = "Game Detail", description = "P2 게임 상세 (6 탭) — 매출/단가")
public class EconomicsController {

    private final EconomicsQueryService service;

    public EconomicsController(EconomicsQueryService service) {
        this.service = service;
    }

    @GetMapping("/{id}/economics")
    @Operation(
            summary = "매출 추정 + 단가",
            description = "ownersMid × price × 0.95 × 0.70 (developer net) 추정. "
                    + "단가 = CCU/시청자, mentions/CCU, CCU/price, price/긍정리뷰. "
                    + "신뢰도는 owners 범위 폭 + 가격 + 신선도로 평가.")
    public EconomicsInsight getEconomicsInsight(@PathVariable long id) {
        return service.getEconomicsInsight(id);
    }
}
