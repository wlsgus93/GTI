package com.gametrend.insight.application.insight;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HallucinationValidatorTest {

    private final HallucinationValidator validator = new HallucinationValidator();

    @Test
    @DisplayName("정상 케이스 — 응답 수치가 모두 InsightContext에 매치 → valid")
    void allNumbersMatch() {
        var ctx = sampleContext();
        // ctx: latestCcu=100000, twitchViewers=50000, totalMentions=12500, ownersMid=21000000
        String response = "CCU 100,000 / Twitch viewers 50,000 / mentions 12,500. owners 21,000,000.";

        var result = validator.validate(response, ctx);

        assertThat(result.valid()).isTrue();
        assertThat(result.fabricatedNumbers()).isEmpty();
        assertThat(result.matchedNumbers()).hasSize(4);
    }

    @Test
    @DisplayName("Fabricated 검출 — 응답에 InsightContext에 없는 수치 등장")
    void fabricatedNumberDetected() {
        var ctx = sampleContext();
        // 9999999는 ctx에 없음
        String response = "CCU 100,000 (good). 시장 매출 9,999,999 USD 예상됨.";

        var result = validator.validate(response, ctx);

        assertThat(result.valid()).isFalse();
        assertThat(result.fabricatedNumbers()).contains("9,999,999");
        assertThat(result.matchedNumbers()).contains("100,000");
    }

    @Test
    @DisplayName("±5% tolerance — 100,000 ↔ 102,000 매치 OK (반올림 흡수)")
    void toleranceWithin5Percent() {
        var ctx = sampleContext();
        String response = "CCU 약 102,000 (반올림).";  // 100,000 +2% 이내

        var result = validator.validate(response, ctx);

        assertThat(result.valid()).isTrue();
        assertThat(result.matchedNumbers()).contains("102,000");
    }

    @Test
    @DisplayName("±5% 초과 — 110,000 (+10%)는 fabricated")
    void toleranceExceeded() {
        var ctx = sampleContext();
        String response = "CCU 110,000 (잘못된 수치).";  // 100,000 +10% — tolerance 초과

        var result = validator.validate(response, ctx);

        assertThat(result.valid()).isFalse();
        assertThat(result.fabricatedNumbers()).contains("110,000");
    }

    @Test
    @DisplayName("3자리 이하 / % 비율 무시 — 검증 대상 X")
    void smallNumbersIgnored() {
        var ctx = sampleContext();
        // "+15%", "1순위", "3-5문장" 같은 자연어는 검증 X
        String response = "Insight: CCU 100,000 +15% 상승 (1순위). Strategy: 3-5문장 요약.";

        var result = validator.validate(response, ctx);

        assertThat(result.valid()).isTrue();
        // 100,000만 매치 — 15, 1, 3, 5는 4자리 미만이라 무시
        assertThat(result.matchedNumbers()).containsExactly("100,000");
    }

    @Test
    @DisplayName("천단위 콤마 정규화 — `1,234,567` = `1234567`")
    void thousandsSeparatorNormalized() {
        var ctx = sampleContext();
        String response1 = "owners 21,000,000";
        String response2 = "owners 21000000";

        assertThat(validator.validate(response1, ctx).valid()).isTrue();
        assertThat(validator.validate(response2, ctx).valid()).isTrue();
    }

    @Test
    @DisplayName("빈 응답 / null → valid (검증할 수치 없음)")
    void emptyResponse() {
        var ctx = sampleContext();
        assertThat(validator.validate(null, ctx).valid()).isTrue();
        assertThat(validator.validate("", ctx).valid()).isTrue();
        assertThat(validator.validate("   ", ctx).valid()).isTrue();
    }

    @Test
    @DisplayName("ctx null / 빈 ctx → 알려진 수치 0 → 모든 수치 fabricated")
    void emptyCtx() {
        var emptyCtx = new InsightContext(
                "Game", null, null, null, null, null, null, null, null, null, null, null, null);
        String response = "CCU 100,000.";

        var result = validator.validate(response, emptyCtx);
        assertThat(result.valid()).isFalse();
        assertThat(result.fabricatedNumbers()).contains("100,000");
    }

    @Test
    @DisplayName("extractKnownNumbers — 4자리 미만 (price 59) 제외")
    void extractIgnoresSmallValues() {
        // priceUsd $59.99는 작은 수치 → 추출 set에 포함 X
        var ctx = sampleContext();
        var known = validator.extractKnownNumbers(ctx);
        assertThat(known).contains(100_000L, 50_000L, 12_500L, 21_000_000L);
        // 59 (priceUsd 단위 환산) 같은 작은 수는 추출 안 됨
        assertThat(known).noneMatch(n -> n < 1000);
    }

    @Test
    @DisplayName("summary() — 디버그 메시지 형식")
    void summaryFormat() {
        var ctx = sampleContext();
        var ok = validator.validate("CCU 100,000.", ctx);
        assertThat(ok.summary()).contains("OK").contains("matched");

        var bad = validator.validate("CCU 9,999,999.", ctx);
        assertThat(bad.summary()).contains("FABRICATED").contains("9,999,999");
    }

    private static InsightContext sampleContext() {
        return new InsightContext(
                "Counter-Strike 2",
                List.of("Action"),
                "Valve",
                100_000,                    // latestCcu
                4.7,                         // ccuDeltaPct (작은 %, 무시)
                50_000,                      // twitchViewers
                12_500,                      // totalMentions
                95.0,                        // reviewScorePercent (%, 무시)
                21_000_000L,                 // ownersMid
                new BigDecimal("59.99"),     // priceUsd (작은 수, 무시)
                new BigDecimal("997333750"), // developerNetRevenue
                2.0,                         // viewToPlayRatio
                "HIGH");
    }
}
