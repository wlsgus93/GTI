package com.gametrend.insight.application.insight;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * LLM 응답 할루시네이션 검증 — 룰 safety.md §1.B (W7 D1).
 *
 * <p>"수치는 DB only" 원칙 강제: LLM 응답에 등장하는 큰 수치(4자리 이상)가
 * {@link InsightContext}의 알려진 값과 ±5% 이내 매치되어야 함.
 *
 * <p>검증 정책:
 * <ul>
 *   <li>4자리 이상 숫자만 (천단위 콤마 포함) — % / 1-3자리 ordinal은 false positive 회피
 *   <li>±5% tolerance — 반올림/단위 변환 오차 흡수
 *   <li>천단위 콤마 정규화 (`1,234,567` = `1234567`)
 *   <li>단위 압축 (`1.2M` = `1200000`)은 미지원 — 향후 정규식 확장 가능
 * </ul>
 *
 * <p>한계 (정직):
 * <ul>
 *   <li>LLM이 "약 5만"으로 변환해서 응답하면 false positive 가능 (`50000` 매치 X if 정확치 없음)
 *   <li>%/ratio는 검증 X — Insight 섹션의 "+15%" 같은 자연어 표현 보존
 *   <li>완전한 보호 X — best-effort 검증 + 의심스러우면 logging
 * </ul>
 */
@Component
public class HallucinationValidator {

    /** 검증 대상 — 4자리 이상 숫자 (천단위 콤마 또는 연속). */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d{1,3}(?:,\\d{3})+\\b|\\b\\d{4,}\\b");

    /** 매칭 허용 오차 (5%). */
    public static final double TOLERANCE = 0.05;

    /**
     * LLM 응답 본문에서 4자리 이상 수치 추출 → 알려진 값과 매칭.
     *
     * @param llmResponse LLM 생성 텍스트
     * @param ctx          InsightContext (DB 수치 — 알려진 값 origin)
     * @return 검증 결과
     */
    public ValidationResult validate(String llmResponse, InsightContext ctx) {
        if (llmResponse == null || llmResponse.isBlank()) {
            return new ValidationResult(true, List.of(), List.of());
        }

        Set<Long> knownNumbers = extractKnownNumbers(ctx);

        List<String> fabricated = new ArrayList<>();
        List<String> matched = new ArrayList<>();

        Matcher m = NUMBER_PATTERN.matcher(llmResponse);
        while (m.find()) {
            String numStr = m.group();
            long parsed;
            try {
                parsed = Long.parseLong(numStr.replace(",", ""));
            } catch (NumberFormatException e) {
                continue; // 비정상 형식 — 무시
            }

            if (matchesAny(parsed, knownNumbers, TOLERANCE)) {
                matched.add(numStr);
            } else {
                fabricated.add(numStr);
            }
        }

        return new ValidationResult(fabricated.isEmpty(), List.copyOf(fabricated), List.copyOf(matched));
    }

    /** InsightContext의 알려진 큰 수치 (4자리 이상) 추출. */
    Set<Long> extractKnownNumbers(InsightContext ctx) {
        Set<Long> result = new HashSet<>();
        if (ctx == null) return result;

        addIfLarge(result, ctx.latestCcu());
        addIfLarge(result, ctx.twitchViewers());
        addIfLarge(result, ctx.totalMentions());
        addIfLarge(result, ctx.ownersMid());
        if (ctx.developerNetRevenue() != null) {
            addIfLarge(result, ctx.developerNetRevenue().longValue());
        }
        // priceUsd는 보통 작아서 검증 대상 X (예: $59.99)
        return result;
    }

    private static void addIfLarge(Set<Long> set, Number n) {
        if (n == null) return;
        long v = n.longValue();
        if (v >= 1000) set.add(v);
    }

    /** parsed 값이 known set 중 어느 것과 ±tolerance 이내 매치되는지. */
    private static boolean matchesAny(long parsed, Set<Long> known, double tolerance) {
        return known.stream().anyMatch(k -> isWithinTolerance(parsed, k, tolerance));
    }

    private static boolean isWithinTolerance(long actual, long expected, double tolerance) {
        if (expected == 0) return actual == 0;
        double ratio = Math.abs((double) actual - expected) / Math.abs(expected);
        return ratio <= tolerance;
    }

    /**
     * @param valid              모든 수치 매치 시 true
     * @param fabricatedNumbers  매치 안 된 수치 (할루시네이션 의심)
     * @param matchedNumbers     매치된 수치 (검증 통과)
     */
    public record ValidationResult(
            boolean valid,
            List<String> fabricatedNumbers,
            List<String> matchedNumbers) {

        /** 디버그용 — fabricated 수치 첫 5개 요약. */
        public String summary() {
            if (valid) return "OK — " + matchedNumbers.size() + " numbers matched";
            List<String> top = fabricatedNumbers.stream()
                    .sorted(Comparator.naturalOrder())
                    .limit(5)
                    .toList();
            return "FABRICATED — " + fabricatedNumbers.size() + " unmatched: " + top;
        }
    }
}
