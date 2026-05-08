package com.gametrend.insight.application.auth;

import com.gametrend.insight.domain.common.DomainException;

/** 회원가입 시 이메일 중복 — 422 매핑. */
public class EmailAlreadyExistsException extends DomainException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }

    @Override
    public String errorCode() {
        return "email-already-exists";
    }
}
