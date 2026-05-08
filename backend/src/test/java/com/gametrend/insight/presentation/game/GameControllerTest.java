package com.gametrend.insight.presentation.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.game.CcuRange;
import com.gametrend.insight.application.game.CcuSeries;
import com.gametrend.insight.application.game.GameDetailItem;
import com.gametrend.insight.application.game.GameNotFoundException;
import com.gametrend.insight.application.game.GameQueryService;
import com.gametrend.insight.application.game.PlayerInsight;
import com.gametrend.insight.config.SecurityConfig;
import com.gametrend.insight.presentation.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * P2 Game Detail REST 슬라이스 테스트. {@code @WebMvcTest}로 컨트롤러만 로드.
 */
@WebMvcTest(GameController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameQueryService gameQueryService;

    @Test
    @DisplayName("GET /api/v1/games/{id} → 200 + 메타 JSON")
    void getDetail_200() throws Exception {
        when(gameQueryService.getDetail(1L)).thenReturn(new GameDetailItem(
                1L, 730L, null, "Counter-Strike 2", null, "Valve", "Valve",
                LocalDate.of(2023, 9, 27), null, List.of(), 1_100_000, 4.76,
                Instant.parse("2026-05-06T00:00:00Z"), Instant.parse("2026-05-06T01:00:00Z")));

        mockMvc.perform(get("/api/v1/games/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Counter-Strike 2"))
                .andExpect(jsonPath("$.steamAppId").value(730))
                .andExpect(jsonPath("$.latestCcu").value(1100000))
                .andExpect(jsonPath("$.ccuDeltaPct").value(4.76));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id} 미존재 → 422 ProblemDetail (game-not-found)")
    void getDetail_notFound_422() throws Exception {
        when(gameQueryService.getDetail(999L)).thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(get("/api/v1/games/999"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Unprocessable Entity"))
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/game-not-found"))
                .andExpect(jsonPath("$.detail").value("Game not found: id=999"));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id}/ccu?range=7d → 200 + 시계열")
    void getCcuSeries_200() throws Exception {
        Instant from = Instant.parse("2026-04-29T00:00:00Z");
        Instant to = Instant.parse("2026-05-06T00:00:00Z");
        when(gameQueryService.getCcuSeries(eq(1L), any(CcuRange.class))).thenReturn(new CcuSeries(
                1L, "7d", from, to,
                List.of(new CcuSeries.Point(Instant.parse("2026-05-05T12:00:00Z"), 1_050_000),
                        new CcuSeries.Point(Instant.parse("2026-05-06T00:00:00Z"), 1_100_000))));

        mockMvc.perform(get("/api/v1/games/1/ccu").param("range", "7d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1))
                .andExpect(jsonPath("$.range").value("7d"))
                .andExpect(jsonPath("$.points.length()").value(2))
                .andExpect(jsonPath("$.points[0].concurrentPlayers").value(1050000))
                .andExpect(jsonPath("$.points[1].concurrentPlayers").value(1100000));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id}/ccu (range 생략) → 30d 기본")
    void getCcuSeries_defaultRange() throws Exception {
        when(gameQueryService.getCcuSeries(eq(1L), eq(CcuRange.DAYS_30)))
                .thenReturn(new CcuSeries(1L, "30d", Instant.now().minusSeconds(86400 * 30), Instant.now(), List.of()));

        mockMvc.perform(get("/api/v1/games/1/ccu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.range").value("30d"));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id}/ccu?range=invalid → 400 ProblemDetail (Day 3 IAE 매핑 적용)")
    void getCcuSeries_invalidRange_400() throws Exception {
        mockMvc.perform(get("/api/v1/games/1/ccu").param("range", "365d"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/bad-request"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("unsupported range")));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id}/players → 200 + 모든 데이터 매핑")
    void getPlayerInsight_200() throws Exception {
        when(gameQueryService.getPlayerInsight(1L)).thenReturn(new PlayerInsight(
                1L,
                new PlayerInsight.PlayerStats(100_000, 95_000, 100_000, 95.0),
                50_000,
                java.util.List.of(
                        new PlayerInsight.MentionByPlatform("YOUTUBE", 12_345, Instant.parse("2026-05-06T01:00:00Z")),
                        new PlayerInsight.MentionByPlatform("REDDIT", 50, Instant.parse("2026-05-06T02:00:00Z"))),
                Instant.parse("2026-05-06T02:00:00Z")));

        mockMvc.perform(get("/api/v1/games/1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(1))
                .andExpect(jsonPath("$.players.concurrentPlayers").value(100000))
                .andExpect(jsonPath("$.players.reviewScorePercent").value(95.0))
                .andExpect(jsonPath("$.twitchViewers").value(50000))
                .andExpect(jsonPath("$.mentions.length()").value(2))
                .andExpect(jsonPath("$.mentions[0].source").value("YOUTUBE"))
                .andExpect(jsonPath("$.mentions[0].mentionCount").value(12345));
    }

    @Test
    @DisplayName("GET /api/v1/games/{id}/players 미존재 → 422 game-not-found")
    void getPlayerInsight_notFound_422() throws Exception {
        when(gameQueryService.getPlayerInsight(999L)).thenThrow(new GameNotFoundException(999L));

        mockMvc.perform(get("/api/v1/games/999/players"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://gametrend.insight/errors/game-not-found"));
    }
}
