package com.gametrend.insight.presentation.verification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.insight.application.verification.CaseDetail;
import com.gametrend.insight.application.verification.CaseSummary;
import com.gametrend.insight.application.verification.StimulusRequest;
import com.gametrend.insight.application.verification.VerificationCaseNotFoundException;
import com.gametrend.insight.application.verification.VerificationService;
import com.gametrend.insight.config.SecurityConfig;
import com.gametrend.insight.domain.verification.CampaignStatus;
import com.gametrend.insight.domain.verification.CaseStatus;
import com.gametrend.insight.domain.verification.Platform;
import com.gametrend.insight.domain.verification.StimulusType;
import com.gametrend.insight.presentation.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class VerificationControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean VerificationService service;

    @Test
    @DisplayName("GET /cases → 200 + 4 케이스 요약")
    void listCases_200() throws Exception {
        when(service.listCases()).thenReturn(List.of(
                new CaseSummary(1L, "C1", "웹캠 표정 호러", CaseStatus.RUNNING, false, 1, 1),
                new CaseSummary(2L, "C2", "음성 격투", CaseStatus.RUNNING, false, 1, 1),
                new CaseSummary(3L, "C3", "LLM NPC", CaseStatus.PLANNING, false, 1, 0),
                new CaseSummary(4L, "C4", "시선 추적 ★", CaseStatus.RUNNING, true, 1, 1)));

        mockMvc.perform(get("/api/v1/verification/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].code").value("C1"))
                .andExpect(jsonPath("$[3].code").value("C4"))
                .andExpect(jsonPath("$[3].priority").value(true));
    }

    @Test
    @DisplayName("GET /cases/{code} → 200 + stimuli + campaigns + KPI")
    void getCase_200() throws Exception {
        var detail = new CaseDetail(
                1L, "C1", "웹캠 표정 호러", "concept...", "hypothesis...", "persona",
                CaseStatus.RUNNING, false,
                Instant.parse("2026-05-06T00:00:00Z"), Instant.parse("2026-05-06T01:00:00Z"),
                List.of(new CaseDetail.StimulusItem(
                        10L, StimulusType.VIDEO, "트레일러", "https://x.invalid", "desc",
                        Instant.parse("2026-05-06T00:00:00Z"))),
                List.of(new CaseDetail.CampaignWithMetrics(
                        20L, 10L, Platform.TWITCH, "Twitch 픽업", "gti_c1_twitch_2026q2",
                        CampaignStatus.RUNNING, Instant.parse("2026-04-29T00:00:00Z"), null,
                        100_000L, 35_000L,
                        53_000L, 1_665L, 110L,
                        0.0314, 0.0661, 660L, 21L)));
        when(service.getCaseDetail("C1")).thenReturn(detail);

        mockMvc.perform(get("/api/v1/verification/cases/C1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("C1"))
                .andExpect(jsonPath("$.stimuli.length()").value(1))
                .andExpect(jsonPath("$.stimuli[0].type").value("VIDEO"))
                .andExpect(jsonPath("$.campaigns[0].ctr").value(0.0314))
                .andExpect(jsonPath("$.campaigns[0].cvr").value(0.0661))
                .andExpect(jsonPath("$.campaigns[0].totalImpressions").value(53000));
    }

    @Test
    @DisplayName("GET /cases/{code} 미존재 → 422 verification-case-not-found")
    void getCase_notFound_422() throws Exception {
        when(service.getCaseDetail("C99"))
                .thenThrow(new VerificationCaseNotFoundException("C99"));

        mockMvc.perform(get("/api/v1/verification/cases/C99"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(
                        "https://gametrend.insight/errors/verification-case-not-found"));
    }

    @Test
    @DisplayName("POST /cases/{code}/stimuli → 201 + 자극물 등록")
    void createStimulus_201() throws Exception {
        var req = new StimulusRequest(StimulusType.LANDING, "C4 랜딩", "https://x.invalid/c4", "접근성 신청 폼");
        var resp = new CaseDetail.StimulusItem(
                100L, StimulusType.LANDING, "C4 랜딩", "https://x.invalid/c4", "접근성 신청 폼",
                Instant.parse("2026-05-06T01:00:00Z"));
        when(service.createStimulus(eq("C4"), any(StimulusRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/verification/cases/C4/stimuli")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.type").value("LANDING"));
    }

    @Test
    @DisplayName("POST /cases/{code}/stimuli — title 누락 → 400 validation")
    void createStimulus_invalid_400() throws Exception {
        String invalidJson = """
                {"type":"VIDEO","title":""}""";

        mockMvc.perform(post("/api/v1/verification/cases/C1/stimuli")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/validation"));
    }
}
