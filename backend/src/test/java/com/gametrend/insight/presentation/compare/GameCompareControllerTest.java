package com.gametrend.insight.presentation.compare;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.compare.CompareItem;
import com.gametrend.insight.application.compare.CompareResult;
import com.gametrend.insight.application.compare.GameCompareService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GameCompareController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GameCompareControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean GameCompareService service;

    @Test
    @DisplayName("GET /compare?ids=1,2,3 → 200 + items + missingGameIds + wallClockMs")
    void compare_200() throws Exception {
        var result = new CompareResult(
                List.of(
                        item(1L, "Counter-Strike 2", 1_100_000),
                        item(2L, "Dota 2", 700_000)),
                List.of(99L),
                42L,
                Instant.parse("2026-05-06T01:00:00Z"));
        when(service.compare(any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/games/compare").param("ids", "1,2,99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].name").value("Counter-Strike 2"))
                .andExpect(jsonPath("$.items[0].latestCcu").value(1100000))
                .andExpect(jsonPath("$.items[0].confidence").value("HIGH"))
                .andExpect(jsonPath("$.missingGameIds[0]").value(99))
                .andExpect(jsonPath("$.wallClockMs").value(42));
    }

    @Test
    @DisplayName("GET /compare?ids=1 → 400 (최소 2개)")
    void tooFewIds_400() throws Exception {
        when(service.compare(any()))
                .thenThrow(new IllegalArgumentException("compare requires at least 2 distinct ids, got 1"));

        mockMvc.perform(get("/api/v1/games/compare").param("ids", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/bad-request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("at least 2")));
    }

    @Test
    @DisplayName("GET /compare?ids=1,2,3,4,5,6 → 400 (최대 5개)")
    void tooManyIds_400() throws Exception {
        when(service.compare(any()))
                .thenThrow(new IllegalArgumentException("compare supports up to 5 ids, got 6"));

        mockMvc.perform(get("/api/v1/games/compare").param("ids", "1,2,3,4,5,6"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("up to 5")));
    }

    private static CompareItem item(long id, String name, int ccu) {
        return new CompareItem(
                id, 730L, name, List.of("Action"), null,
                ccu, 4.7, 50_000, 12_500, 95.0,
                75_000_000L, new BigDecimal("0.00"), new BigDecimal("0.00"),
                "HIGH");
    }
}
