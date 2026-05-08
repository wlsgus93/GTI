package com.gametrend.insight.application.auth;

import com.gametrend.insight.domain.common.DomainException;

/**
 * 인증 실패 — 401 매핑. 보안상 "이메일 없음" / "비밀번호 틀림" 구분 X.
 */
public class AuthException extends DomainException {

    public AuthException(String message) {
        super(message);
    }

    @Override
    public String errorCode() {
        return "auth-failed";
    }
}
