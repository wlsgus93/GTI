package com.gametrend.insight.application.dimension.factor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gametrend.insight.application.insight.LlmClient;
import com.gametrend.insight.application.insight.LlmResponse;
import com.gametrend.insight.application.insight.LlmTask;
import com.gametrend.insight.domain.dimension.factor.HitFactor;
import com.gametrend.insight.domain.dimension.factor.HitFactor.Confidence;
import com.gametrend.insight.domain.dimension.factor.HitFactor.FactorKeyword;
import com.gametrend.insight.domain.dimension.factor.HitFactor.SentimentBaseline;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaEntity;
import com.gametrend.insight.infrastructure.persistence.game.GameJpaRepository;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaEntity;
import com.gametrend.insight.infrastructure.persistence.snapshot.MentionSnapshotJpaRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * D6 산출 서비스 — 게임 메타 + sentiment 분포 → LLM 추론 (Groq Llama 70B FAST task).
 *
 * <p>V1 한계: raw 리뷰 텍스트 X (Steam appreviews 어댑터 미구현) → 메타 + sentiment 기반 추론.
 * 응답에 sourceLabel = "ESTIMATED_FROM_METADATA" + Confidence MEDIUM 표기.
 */
@Service
public class HitFactorService {

    private static final Logger log = LoggerFactory.getLogger(HitFactorService.class);

    private static final int MENTION_SAMPLE_SIZE = 50;
    private static final int LLM_MAX_TOKENS = 1024;

    private final GameJpaRepository gameRepo;
    private final MentionSnapshotJpaRepository mentionRepo;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public HitFactorService(GameJpaRepository gameRepo, MentionSnapshotJpaRepository mentionRepo,
            LlmClient llmClient, ObjectMapper objectMapper) {
        this.gameRepo = gameRepo;
        this.mentionRepo = mentionRepo;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public HitFactor analyze(long gameId) {
        GameJpaEntity game = gameRepo.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("Game not found: " + gameId));

        // sentiment 분포 집계
        List<MentionSnapshotJpaEntity> recent = mentionRepo.findByGameIdOrderByCapturedAtDesc(
                gameId, PageRequest.of(0, MENTION_SAMPLE_SIZE));
        SentimentBaseline baseline = aggregateSentiment(recent);

        // LLM 호출
        String systemPrompt = buildSystemPrompt(game, baseline);
        String userPrompt = "다음 게임의 흥행 요인을 추정하라:\n게임명: " + game.getName()
                + "\n개발사: " + (game.getDeveloper() != null ? game.getDeveloper() : "-")
                + "\n발행사: " + (game.getPublisher() != null ? game.getPublisher() : "-")
                + "\n총 멘션: " + baseline.totalMentions()
                + "\n긍정 비율: " + String.format("%.2f", baseline.positiveRatio())
                + "\n부정 비율: " + String.format("%.2f", baseline.negativeRatio());

        LlmResponse llmResp;
        try {
            llmResp = llmClient.complete(LlmTask.KEYWORD_EXTRACT, systemPrompt, userPrompt, LLM_MAX_TOKENS);
        } catch (RuntimeException e) {
            log.error("D6 LLM call failed for gameId={}: {}", gameId, e.getMessage());
            return emptyResult(gameId, game.getName(), baseline, "LLM call failed");
        }

        // JSON 파싱
        return parseLlmJson(gameId, game.getName(), baseline, llmResp);
    }

    private static SentimentBaseline aggregateSentiment(List<MentionSnapshotJpaEntity> recent) {
        if (recent.isEmpty()) return new SentimentBaseline(0, 0.0, 0.0, 0.0);
        Map<String, Integer> bySentiment = new HashMap<>();
        int total = 0;
        for (MentionSnapshotJpaEntity m : recent) {
            String sent = m.getSentiment() != null ? m.getSentiment().name() : "NEU";
            bySentiment.merge(sent, m.getMentionCount(), Integer::sum);
            total += m.getMentionCount();
        }
        if (total == 0) return new SentimentBaseline(0, 0.0, 0.0, 0.0);
        double pos = bySentiment.getOrDefault("POS", 0) / (double) total;
        double neu = bySentiment.getOrDefault("NEU", 0) / (double) total;
        double neg = bySentiment.getOrDefault("NEG", 0) / (double) total;
        return new SentimentBaseline(total, pos, neu, neg);
    }

    private static String buildSystemPrompt(GameJpaEntity game, SentimentBaseline baseline) {
        return "너는 GTI 게임 시장 분석가다. 주어진 게임 메타 + 커뮤니티 sentiment 분포에서 "
                + "이 게임의 흥행 요인을 추정한다.\n"
                + "**중요 — 정확한 JSON 만 출력. 그 외 텍스트 / markdown / 코드블록 금지**:\n"
                + "{\n"
                + "  \"positive\": [{\"keyword\":\"<짧게>\",\"reasoning\":\"<1줄>\"}, ...],\n"
                + "  \"negative\": [{\"keyword\":\"<짧게>\",\"reasoning\":\"<1줄>\"}, ...],\n"
                + "  \"summary\": \"<1줄 요약>\"\n"
                + "}\n"
                + "각 배열 최대 5개. 키워드는 '스토리', '그래픽', '가격' 같은 짧은 명사. "
                + "한국어로. raw 리뷰 텍스트가 아닌 메타 + sentiment 기반 추론임을 인지. "
                + "추정 신뢰도 낮음 — 자신감 X 표현 ('~로 추정', '~할 가능성').";
    }

    private HitFactor parseLlmJson(long gameId, String gameName, SentimentBaseline baseline, LlmResponse resp) {
        try {
            String content = resp.content().trim();
            // markdown 코드블록 제거
            if (content.startsWith("```")) {
                int firstNl = content.indexOf('\n');
                int lastBackticks = content.lastIndexOf("```");
                if (firstNl > 0 && lastBackticks > firstNl) {
                    content = content.substring(firstNl + 1, lastBackticks).trim();
                }
            }
            JsonNode root = objectMapper.readTree(content);
            List<FactorKeyword> positive = parseKeywords(root.get("positive"));
            List<FactorKeyword> negative = parseKeywords(root.get("negative"));
            String summary = root.has("summary") ? root.get("summary").asText() : "";
            return new HitFactor(gameId, gameName, positive, negative, summary,
                    resp.model(), "ESTIMATED_FROM_METADATA", Confidence.MEDIUM, baseline, Instant.now());
        } catch (Exception e) {
            log.warn("D6 JSON parse failed for gameId={}: {} — content={}", gameId, e.getMessage(),
                    resp.content().substring(0, Math.min(200, resp.content().length())));
            return emptyResult(gameId, gameName, baseline, "JSON parse failed");
        }
    }

    private static List<FactorKeyword> parseKeywords(JsonNode arr) {
        List<FactorKeyword> result = new ArrayList<>();
        if (arr == null || !arr.isArray()) return result;
        Iterator<JsonNode> it = arr.elements();
        while (it.hasNext()) {
            JsonNode el = it.next();
            String keyword = el.has("keyword") ? el.get("keyword").asText() : "";
            String reasoning = el.has("reasoning") ? el.get("reasoning").asText() : "";
            if (!keyword.isEmpty()) {
                result.add(new FactorKeyword(keyword, reasoning));
            }
        }
        return result;
    }

    private static HitFactor emptyResult(long gameId, String gameName,
            SentimentBaseline baseline, String reason) {
        return new HitFactor(gameId, gameName, List.of(), List.of(), reason,
                "fallback", "ERROR", Confidence.LOW, baseline, Instant.now());
    }
}
