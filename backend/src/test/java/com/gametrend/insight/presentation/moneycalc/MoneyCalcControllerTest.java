package com.gametrend.insight.presentation.moneycalc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.moneycalc.MoneyCalcResult;
import com.gametrend.insight.application.moneycalc.MoneyCalcService;
import com.gametrend.insight.config.SecurityConfig;
import com.gametrend.insight.presentation.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
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

@WebMvcTest(MoneyCalcController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MoneyCalcControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MoneyCalcService service;

    @Test
    @DisplayName("POST /simulate — 정상 입력 → 200 + assumptions/monteCarlo 매핑")
    void simulate_200() throws Exception {
        var stub = new MoneyCalcResult(
                null, null, null,
                new MoneyCalcResult.MonteCarloOutput(
                        100, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.ZERO, BigDecimal.ZERO, 0.0),
                List.of(),
                new MoneyCalcResult.Assumptions(0.05, 0.30, 0, 42L),
                Instant.now());
        when(service.simulate(any())).thenReturn(stub);

        var json = """
                {
                  "pessimistic": {"owners": 50000, "priceCents": 1999},
                  "realistic":   {"owners": 200000, "priceCents": 1999},
                  "optimistic":  {"owners": 800000, "priceCents": 1999},
                  "developmentCostCents": 50000000,
                  "marketingCostCents": 10000000,
                  "monteCarloIterations": 100
                }""";

        mockMvc.perform(post("/api/v1/moneycalc/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assumptions.refundRate").value(0.05))
                .andExpect(jsonPath("$.assumptions.steamCut").value(0.30))
                .andExpect(jsonPath("$.monteCarlo.iterations").value(100));
    }

    @Test
    @DisplayName("POST /simulate — pessimistic 누락 → 400 validation (서비스 호출 안 됨)")
    void missingScenario_400() throws Exception {
        var json = """
                {
                  "realistic": {"owners": 100000, "priceCents": 1999},
                  "optimistic": {"owners": 200000, "priceCents": 1999},
                  "developmentCostCents": 1000,
                  "marketingCostCents": 0
                }""";

        mockMvc.perform(post("/api/v1/moneycalc/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/validation"));
    }

    @Test
    @DisplayName("POST /simulate — 시나리오 순서 위반 → service IAE → 400")
    void invalidOrder_400() throws Exception {
        when(service.simulate(any()))
                .thenThrow(new IllegalArgumentException("pessimistic.owners must be ≤ realistic.owners"));

        var json = """
                {
                  "pessimistic": {"owners": 500000, "priceCents": 1999},
                  "realistic":   {"owners": 100000, "priceCents": 1999},
                  "optimistic":  {"owners": 800000, "priceCents": 1999},
                  "developmentCostCents": 1000,
                  "marketingCostCents": 0
                }""";

        mockMvc.perform(post("/api/v1/moneycalc/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/bad-request"))
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("pessimistic.owners")));
    }
}
