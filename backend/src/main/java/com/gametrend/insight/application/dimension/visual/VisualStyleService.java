package com.gametrend.insight.application.dimension.visual;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.insight.domain.dimension.visual.VisualStyle;
import com.gametrend.insight.infrastructure.llm.GeminiVisionClient;
import com.gametrend.insight.infrastructure.llm.GeminiVisionClient.VisionResponse;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * D3 산출 서비스 — Gemini Vision 으로 게임 cover 이미지 분류.
 *
 * <p>cover_image_url 가 없으면 503 또는 빈 응답.
 */
@Service
public class VisualStyleService {

    private static final Logger log = LoggerFactory.getLogger(VisualStyleService.class);
    private static final int VISION_MAX_TOKENS = 512;

    private final GameJpaRepository gameRepo;
    private final GeminiVisionClient visionClient;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    public VisualStyleService(GameJpaRepository gameRepo, GeminiVisionClient visionClient,
            ObjectMapper objectMapper) {
        this.gameRepo = gameRepo;
        this.visionClient = visionClient;
        this.objectMapper = objectMapper;
    }

    public VisualStyle analyze(long gameId) {
        GameJpaEntity game = gameRepo.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        String imageUrl = game.getCoverImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            return emptyResult(gameId, game.getName(), "cover_image_url 미설정");
        }

        if (visionClient == null) {
            return emptyResult(gameId, game.getName(), "GeminiVisionClient 미활성 (provider=router/gemini 필요)");
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = "이 게임 (" + game.getName() + ") 의 cover 이미지를 분석하라.";

        VisionResponse resp;
        try {
            resp = visionClient.analyze(imageUrl, systemPrompt, userPrompt, VISION_MAX_TOKENS);
        } catch (RuntimeException e) {
            log.error("D3 Vision call failed gameId={}: {}", gameId, e.getMessage());
            return emptyResult(gameId, game.getName(), "Vision call failed: " + e.getMessage());
        }

        return parseLlmJson(gameId, game.getName(), imageUrl, resp);
    }

    private static String buildSystemPrompt() {
        return "너는 게임 비주얼 분석가다. 이미지의 아트 스타일을 분류하라.\n"
                + "**중요 — 정확한 JSON 만 출력. 그 외 텍스트 / markdown / 코드블록 금지**:\n"
                + "{\n"
                + "  \"style\": \"<픽셀|셀룰러|사실주의|카툰|3D|2D 일러스트|미니멀리스트>\",\n"
                + "  \"colorTone\": \"<warm|cool|neutral>\",\n"
                + "  \"mood\": \"<어두운|밝은|중립>\",\n"
                + "  \"dominantColors\": [\"#hex\", \"#hex\"],\n"
                + "  \"confidence\": 0.0~1.0,\n"
                + "  \"reasoning\": \"<1줄 근거>\"\n"
                + "}";
    }

    private VisualStyle parseLlmJson(long gameId, String gameName, String imageUrl, VisionResponse resp) {
        try {
            String content = resp.content().trim();
            if (content.startsWith("```")) {
                int firstNl = content.indexOf('\n');
                int lastBackticks = content.lastIndexOf("```");
                if (firstNl > 0 && lastBackticks > firstNl) {
                    content = content.substring(firstNl + 1, lastBackticks).trim();
                }
            }
            JsonNode root = objectMapper.readTree(content);
            String style = root.path("style").asText("UNKNOWN");
            String colorTone = root.path("colorTone").asText("neutral");
            String mood = root.path("mood").asText("중립");
            double confidence = root.path("confidence").asDouble(0.5);
            String reasoning = root.path("reasoning").asText("");
            List<String> colors = parseColors(root.get("dominantColors"));
            return new VisualStyle(gameId, gameName, imageUrl, style, colorTone, mood,
                    colors, confidence, reasoning, resp.model(), Instant.now());
        } catch (Exception e) {
            log.warn("D3 JSON parse failed gameId={}: {} — content head={}", gameId, e.getMessage(),
                    resp.content().substring(0, Math.min(200, resp.content().length())));
            return emptyResult(gameId, gameName, "JSON parse failed");
        }
    }

    private static List<String> parseColors(JsonNode arr) {
        List<String> result = new ArrayList<>();
        if (arr == null || !arr.isArray()) return result;
        Iterator<JsonNode> it = arr.elements();
        while (it.hasNext()) result.add(it.next().asText());
        return result;
    }

    private static VisualStyle emptyResult(long gameId, String gameName, String reason) {
        return new VisualStyle(gameId, gameName, null, "UNKNOWN", "neutral", "중립",
                List.of(), 0.0, reason, "fallback", Instant.now());
    }
}
