package com.gametrend.insight.domain.verification;

/** 검증 케이스 진행 상태 — Pretotyping lifecycle. */
public enum CaseStatus {
    /** 가설 정의 + 자극물 준비 단계. */
    PLANNING,
    /** 캠페인 가동 중 — 데이터 수집. */
    RUNNING,
    /** 캠페인 종료 후 데이터 분석 중. */
    ANALYZING,
    /** 결론 도출 — 채택/기각/피벗. */
    DONE
}
