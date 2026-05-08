package com.gametrend.insight.presentation.verification;

import com.gametrend.insight.application.verification.CampaignImpactAnalysis;
import com.gametrend.insight.application.verification.CampaignRequest;
import com.gametrend.insight.application.verification.CaseDetail;
import com.gametrend.insight.application.verification.CaseSummary;
import com.gametrend.insight.application.verification.StimulusRequest;
import com.gametrend.insight.application.verification.VerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * P7 검증 모듈 ★ — Pretotyping 4 케이스 (C1~C4) + 자극물 + 캠페인.
 *
 * <p>인터뷰 STT / CommunityMention / Cohort 분석은 W4 후속.
 */
@RestController
@RequestMapping("/api/v1/verification")
@Tag(name = "Verification (P7)", description = "Pretotyping 검증 — 4 케이스 + 자극물 + 캠페인")
public class VerificationController {

    private final VerificationService service;

    public VerificationController(VerificationService service) {
        this.service = service;
    }

    @GetMapping("/cases")
    @Operation(summary = "4 케이스 요약 목록 (C1~C4)")
    public List<CaseSummary> listCases() {
        return service.listCases();
    }

    @GetMapping("/cases/{code}")
    @Operation(summary = "단일 케이스 상세 + 자극물 + 캠페인 (KPI 포함)",
            description = "각 캠페인은 누적 metrics에서 CTR/CVR/CPM/CPC 자동 계산")
    public CaseDetail getCase(@PathVariable String code) {
        return service.getCaseDetail(code);
    }

    @PostMapping("/cases/{code}/stimuli")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "자극물 등록")
    public CaseDetail.StimulusItem createStimulus(
            @PathVariable String code, @Valid @RequestBody StimulusRequest req) {
        return service.createStimulus(code, req);
    }

    @PostMapping("/cases/{code}/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "캠페인 시작 (status=SCHEDULED으로 생성, 별도 API로 RUNNING 전이 — W4 후속)")
    public CaseDetail.CampaignWithMetrics createCampaign(
            @PathVariable String code, @Valid @RequestBody CampaignRequest req) {
        return service.createCampaign(code, req);
    }

    @GetMapping("/campaigns/{campaignId}/impact")
    @Operation(
            summary = "시차 상관 분석 (W7 D2 — 마케터 페르소나)",
            description =
                    "캠페인 클릭수 vs 게임 CCU 일별 시계열의 Pearson 상관을 lag 0~maxLag에서 측정. "
                            + "최적 lag + 자연어 해석 + 신뢰도 응답. "
                            + "상관 ≠ 인과 — 다른 요인 (Steam 세일/이벤트) 통제 후 추가 검증 필요.")
    public CampaignImpactAnalysis getCampaignImpact(
            @PathVariable long campaignId,
            @RequestParam long gameId,
            @RequestParam(required = false, defaultValue = "14") int maxLag) {
        return service.analyzeCampaignImpact(campaignId, gameId, maxLag);
    }
}
