package com.gametrend.insight.presentation.economics;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.economics.Confidence;
import com.gametrend.insight.application.economics.EconomicsInsight;
import com.gametrend.insight.application.economics.EconomicsQueryService;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.config.SecurityConfig;
import com.gametrend.insight.presentation.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EconomicsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EconomicsControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean EconomicsQueryService service;

    @Test
    @DisplayName("GET /economics → 200 + JSON 매핑 (revenue + unit + confidence)")
    void get_200() throws Exception {
        var insight = new EconomicsInsight(
                1L,
                new EconomicsInsight.RevenueEstimate(
                        20_000_000L, 22_000_000L, 21_000_000L,
                        new BigDecimal("59.99"),
                        new BigDecimal("1259790000.00"),
                        new BigDecimal("1196800500.00"),
                        new BigDecimal("837760350.00"),
                        1_600_000, 5_600_000),
                new EconomicsInsight.UnitEconomics(2.0, 0.125, 1666.944, new BigDecimal("0.0006")),
                Confidence.HIGH,
                Instant.parse("2026-05-06T01:00:00Z"));

        when(service.getEconomicsInsight(1L)).thenReturn(insight);

        mockMvc.perform(get("/api/v1/games/1/economics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1))
                .andExpect(jsonPath("$.revenue.ownersMid").value(21000000))
                .andExpect(jsonPath("$.revenue.priceUsd").value(59.99))
                .andExpect(jsonPath("$.revenue.estimatedDau").value(1600000))
                .andExpect(jsonPath("$.unitEconomics.viewToPlayRatio").value(2.0))
                .andExpect(jsonPath("$.unitEconomics.mentionToPlayRatio").value(0.125))
                .andExpect(jsonPath("$.confidence").value("HIGH"));
    }

    @Test
    @DisplayName("GET /economics 미존재 → 422 game-not-found")
    void notFound_422() throws Exception {
        when(service.getEconomicsInsight(999L)).thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(get("/api/v1/games/999/economics"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/game-not-found"));
    }

    @Test
    @DisplayName("GET /economics — owners 없을 때 revenue null, unit/confidence는 응답")
    void partialData() throws Exception {
        var insight = new EconomicsInsight(
                1L, null,
                new EconomicsInsight.UnitEconomics(null, null, 1000.0, null),
                Confidence.LOW,
                Instant.parse("2026-05-06T01:00:00Z"));
        when(service.getEconomicsInsight(1L)).thenReturn(insight);

        mockMvc.perform(get("/api/v1/games/1/economics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").doesNotExist())
                .andExpect(jsonPath("$.unitEconomics.priceEfficiency").value(1000.0))
                .andExpect(jsonPath("$.confidence").value("LOW"));
    }
}
