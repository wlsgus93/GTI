package com.gametrend.insight.presentation.insight;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.insight.GameInsight;
import com.gametrend.insight.application.insight.InsightService;
import com.gametrend.insight.config.SecurityConfig;
import com.gametrend.insight.presentation.exception.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InsightController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class InsightControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean InsightService service;

    @Test
    @DisplayName("GET /insight → 200 + GameInsight JSON")
    void get_200() throws Exception {
        var insight = new GameInsight(
                1L,
                "최근 24h CCU +4.7% 상승. Twitch 시청자 5만 → 시청→플레이 전환 양호. ...",
                "INSIGHT_V1",
                1550,
                "claude-opus-4-5",
                false,
                false,
                Instant.parse("2026-05-06T01:00:00Z"),
                Instant.parse("2026-05-07T01:00:00Z"));
        when(service.getOrGenerate(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(com.gametrend.insight.domain.insight.Persona.class))).thenReturn(insight);

        mockMvc.perform(get("/api/v1/games/1/insight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1))
                .andExpect(jsonPath("$.summary").value(org.hamcrest.Matchers.containsString("CCU +4.7%")))
                .andExpect(jsonPath("$.promptVersion").value("INSIGHT_V1"))
                .andExpect(jsonPath("$.totalTokens").value(1550))
                .andExpect(jsonPath("$.model").value("claude-opus-4-5"))
                .andExpect(jsonPath("$.cached").value(false));
    }

    @Test
    @DisplayName("GET /insight 미존재 → 422 game-not-found")
    void notFound_422() throws Exception {
        when(service.getOrGenerate(org.mockito.ArgumentMatchers.eq(999L),
                org.mockito.ArgumentMatchers.any(com.gametrend.insight.domain.insight.Persona.class)))
                .thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(get("/api/v1/games/999/insight"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/game-not-found"));
    }

    @Test
    @DisplayName("GET /insight LLM 장애 + stale도 없음 → 503 + Retry-After (W3 D1)")
    void llmUnavailable_503() throws Exception {
        when(service.getOrGenerate(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(com.gametrend.insight.domain.insight.Persona.class)))
                .thenThrow(new com.gametrend.insight.application.insight.LlmUnavailableException(
                        1L, new RuntimeException("anthropic timeout")));

        mockMvc.perform(get("/api/v1/games/1/insight"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/llm-unavailable"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(60));
    }

    @Test
    @DisplayName("GET /insight stale fallback → 200 + stale=true (W3 D1)")
    void stale_200() throws Exception {
        var stale = new GameInsight(
                1L, "(stale) 옛 분석 본문", "INSIGHT_V1", 1500, "claude-opus-4-5",
                true, true, // cached=true, stale=true
                Instant.parse("2026-05-04T00:00:00Z"),
                Instant.parse("2026-05-05T00:00:00Z")); // 이미 expired
        when(service.getOrGenerate(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(com.gametrend.insight.domain.insight.Persona.class))).thenReturn(stale);

        mockMvc.perform(get("/api/v1/games/1/insight"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true))
                .andExpect(jsonPath("$.stale").value(true));
    }
}
