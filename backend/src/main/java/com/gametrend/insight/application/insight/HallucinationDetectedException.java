package com.gametrend.insight.application.insight;

import com.gametrend.insight.domain.common.DomainException;

/**
 * LLM 응답에 fabricated 수치 검출 + 재호출 후에도 지속 → 503.
 *
 * <p>룰 safety.md §1.B 적용 (W7 D1). 재호출 1회 후 valid 못 나오면 응답 거부.
 *
 * <p>발생 조건:
 * <ol>
 *   <li>LLM 응답에 InsightContext에 없는 4자리 이상 수치 등장
 *   <li>HallucinationValidator가 fabricated로 판정
 *   <li>재호출 1회 시도 — 그래도 fabricated
 * </ol>
 */
public class HallucinationDetectedException extends DomainException {

    private final long gameId;
    private final java.util.List<String> fabricatedNumbers;

    public HallucinationDetectedException(long gameId, java.util.List<String> fabricatedNumbers) {
        super("LLM response contains fabricated numbers (gameId="
                + gameId + "): " + fabricatedNumbers);
        this.gameId = gameId;
        this.fabricatedNumbers = fabricatedNumbers;
    }

    @Override
    public String errorCode() {
        return "hallucination-detected";
    }

    public long getGameId() {
        return gameId;
    }

    public java.util.List<String> getFabricatedNumbers() {
        return fabricatedNumbers;
    }
}
