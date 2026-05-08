package com.gametrend.insight.presentation.dimension;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gametrend.insight.application.dimension.release.ReleaseDimension;
import com.gametrend.insight.application.dimension.release.ReleaseDimensionService;
import com.gametrend.insight.config.SecurityConfig;
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

@WebMvcTest(DimensionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class DimensionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ReleaseDimensionService service;
    @MockitoBean com.gametrend.insight.application.dimension.community.CommunityDimensionService communityService;

    @Test
    @DisplayName("GET /dimensions/d1-release → 200 + byGenre + byYear")
    void get_200() throws Exception {
        var dim = new ReleaseDimension(
                List.of(
                        new ReleaseDimension.GenreStats("Action", 3, 666_666, 1_100_000, "CS2", 1L, 0L, 2L),
                        new ReleaseDimension.GenreStats("RPG", 2, 125_000, 200_000, "Elden Ring", 0L, 0L, 2L)),
                List.of(
                        new ReleaseDimension.YearStats(2023, 1, 1_100_000),
                        new ReleaseDimension.YearStats(2022, 1, 200_000)),
                5,
                Instant.parse("2026-05-06T01:00:00Z"));
        when(service.getReleaseDimension()).thenReturn(dim);

        mockMvc.perform(get("/api/v1/dimensions/d1-release"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGames").value(5))
                .andExpect(jsonPath("$.byGenre.length()").value(2))
                .andExpect(jsonPath("$.byGenre[0].genre").value("Action"))
                .andExpect(jsonPath("$.byGenre[0].gameCount").value(3))
                .andExpect(jsonPath("$.byGenre[0].topGameName").value("CS2"))
                .andExpect(jsonPath("$.byYear[0].year").value(2023));
    }
}
