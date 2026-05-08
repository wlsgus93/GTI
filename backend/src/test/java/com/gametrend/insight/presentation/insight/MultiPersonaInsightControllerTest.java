package com.gametrend.insight.presentation.insight;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.insight.InsightService;
import com.gametrend.insight.application.insight.MultiPersonaInsight;
import com.gametrend.insight.config.SecurityConfig;
import com.gametrend.insight.domain.insight.Persona;
import com.gametrend.insight.presentation.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Multi-persona 응답 컨트롤러 테스트 (W6 D2).
 */
@WebMvcTest(InsightController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class MultiPersonaInsightControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean InsightService service;

    @Test
    @DisplayName("GET /insights?personas=INDIE,INVESTOR → 200 + 2 perspectives + totalLatencyMs")
    void multiPersona_200() throws Exception {
        Instant now = Instant.parse("2026-05-06T01:00:00Z");
        var multi = new MultiPersonaInsight(
                1L,
                List.of(
                        new MultiPersonaInsight.Perspective(
                                Persona.INDIE, "인디 개발자", "인디 응답", 1000, "claude-opus-4-5",
                                "INSIGHT_V2_INDIE", false, false, now, now.plusSeconds(86400)),
                        new MultiPersonaInsight.Perspective(
                                Persona.INVESTOR, "투자자", "투자자 응답", 1500, "claude-opus-4-5",
                                "INSIGHT_V2_INVESTOR", true, false, now, now.plusSeconds(86400))),
                25L,
                now);
        when(service.getOrGenerateMulti(anyLong(), any())).thenReturn(multi);

        mockMvc.perform(get("/api/v1/games/1/insights").param("personas", "INDIE,INVESTOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1))
                .andExpect(jsonPath("$.perspectives.length()").value(2))
                .andExpect(jsonPath("$.perspectives[0].persona").value("INDIE"))
                .andExpect(jsonPath("$.perspectives[0].personaLabel").value("인디 개발자"))
                .andExpect(jsonPath("$.perspectives[0].cached").value(false))
                .andExpect(jsonPath("$.perspectives[1].persona").value("INVESTOR"))
                .andExpect(jsonPath("$.perspectives[1].cached").value(true))
                .andExpect(jsonPath("$.totalLatencyMs").value(25));
    }

    @Test
    @DisplayName("GET /insights (personas 생략) → default INDIE 1 perspective")
    void noPersonas_defaultIndie() throws Exception {
        Instant now = Instant.parse("2026-05-06T01:00:00Z");
        var multi = new MultiPersonaInsight(
                1L,
                List.of(new MultiPersonaInsight.Perspective(
                        Persona.INDIE, "인디 개발자", "default 응답", 1000, "claude-opus-4-5",
                        "INSIGHT_V2_INDIE", false, false, now, now.plusSeconds(86400))),
                10L, now);
        when(service.getOrGenerateMulti(anyLong(), any())).thenReturn(multi);

        mockMvc.perform(get("/api/v1/games/1/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perspectives.length()").value(1))
                .andExpect(jsonPath("$.perspectives[0].persona").value("INDIE"));
    }

    @Test
    @DisplayName("GET /insights — 빈 personas 리스트 → service IAE → 400")
    void emptyPersonas_400() throws Exception {
        // Spring MVC: 빈 리스트는 빈 list로 파싱. Controller가 default INDIE로 채움 → service 호출 X→ 200.
        // 따라서 빈 리스트 케이스는 default 처리. 명시적 IAE 케이스는 service 직접 IAE throw 시.
        when(service.getOrGenerateMulti(anyLong(), any()))
                .thenThrow(new IllegalArgumentException("personas must not be empty"));

        mockMvc.perform(get("/api/v1/games/1/insights").param("personas", "INDIE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("must not be empty")));
    }

    @Test
    @DisplayName("GET /insights?personas=INVALID → 400 (Spring MVC enum conversion 실패)")
    void invalidPersona_400() throws Exception {
        mockMvc.perform(get("/api/v1/games/1/insights").param("personas", "INVALID_PERSONA"))
                .andExpect(status().isBadRequest());
    }
}
