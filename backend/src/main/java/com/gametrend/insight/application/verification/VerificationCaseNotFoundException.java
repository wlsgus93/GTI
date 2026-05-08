package com.gametrend.insight.application.verification;

import com.gametrend.insight.domain.common.DomainException;

/** P7 검증 케이스 미존재 — 422 매핑. */
public class VerificationCaseNotFoundException extends DomainException {

    private final String code;

    public VerificationCaseNotFoundException(String code) {
        super("Verification case not found: code=" + code);
        this.code = code;
    }

    @Override
    public String errorCode() {
        return "verification-case-not-found";
    }

    public String getCode() {
        return code;
    }
}
