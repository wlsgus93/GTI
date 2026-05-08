package com.gametrend.insight.domain.dimension.visual;

import java.time.Instant;
import java.util.List;

/**
 * D3 — 그래픽 성향 (Visual Style).
 *
 * <p>입력: 게임 cover/header 이미지 URL → Gemini 2.5 Flash Vision 호출.
 * 산출: 아트 스타일 + 색감 + 분위기 + 주요 색상 + confidence.
 *
 * <p>분류 카테고리 (system prompt 강제):
 * <ul>
 *   <li>style: 픽셀 / 셀룰러 / 사실주의 / 카툰 / 3D / 2D 일러스트 / 미니멀리스트
 *   <li>colorTone: warm / cool / neutral
 *   <li>mood: 어두운 / 밝은 / 중립
 * </ul>
 */
public record VisualStyle(
        long gameId,
        String gameName,
        String imageUrl,
        String style,
        String colorTone,
        String mood,
        List<String> dominantColors,
        double confidence,
        String reasoning,
        String model,
        Instant generatedAt) {
}
