package com.gametrend.insight.presentation.moneycalc;

import com.gametrend.insight.application.moneycalc.MoneyCalcRequest;
import com.gametrend.insight.application.moneycalc.MoneyCalcResult;
import com.gametrend.insight.application.moneycalc.MoneyCalcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * P5 MoneyCalc REST.
 *
 * <p>예: 게임 발매 의사결정 시뮬레이터 — 가격/판매량/비용 입력 → ROI/BEP/Monte Carlo/민감도.
 */
@RestController
@RequestMapping("/api/v1/moneycalc")
@Tag(name = "MoneyCalc (P5)", description = "Pretotyping 의사결정 시뮬레이터 — 매출 시나리오 + Monte Carlo + 민감도")
public class MoneyCalcController {

    private final MoneyCalcService service;

    public MoneyCalcController(MoneyCalcService service) {
        this.service = service;
    }

    @PostMapping("/simulate")
    @Operation(
            summary = "매출 시뮬레이션 — 3 시나리오 + Monte Carlo + 민감도",
            description = "비관/보통/낙관 owners×price + 개발/마케팅 비용 → "
                    + "Monte Carlo 1000회 (Triangular distribution) + ROI/BEP/profit probability + "
                    + "OAT 민감도 (owners vs priceCents). "
                    + "monteCarloIterations 미지정 시 1000 (최대 10000).")
    public MoneyCalcResult simulate(@Valid @RequestBody MoneyCalcRequest req) {
        return service.simulate(req);
    }
}
