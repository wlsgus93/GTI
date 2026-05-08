package com.gametrend.insight.domain.insight;

/**
 * AI 분석 종류. {@code analysis.kind} 컬럼에 VARCHAR(20)로 저장.
 *
 * <ul>
 *   <li>INSIGHT_BRIEF — P2 게임 상세 1분 요약 (Day 5 도입)
 *   <li>ANOMALY — 이상 탐지 결과 (W3+)
 *   <li>COMPETITIVE — 경쟁 분석 (W3+)
 * </ul>
 */
public enum AnalysisKind {
    INSIGHT_BRIEF,
    ANOMALY,
    COMPETITIVE
}
