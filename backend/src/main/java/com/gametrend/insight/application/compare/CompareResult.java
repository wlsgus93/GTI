package com.gametrend.insight.application.compare;

import java.time.Instant;
import java.util.List;

/**
 * P3 게임 비교 응답.
 *
 * @param items         비교 가능한 게임 (요청 순서 유지)
 * @param missingGameIds 미존재 또는 조회 실패 ID — graceful degradation
 * @param wallClockMs   서버 측 병렬 호출 wall-clock (정량 지표 노출, 디버그/감사용)
 * @param generatedAt   응답 생성 시각
 */
public record CompareResult(
        List<CompareItem> items,
        List<Long> missingGameIds,
        long wallClockMs,
        Instant generatedAt) {
}
