package com.gametrend.insight.application.auth;

/**
 * 회원가입/로그인 성공 응답.
 *
 * @param token       JWT (Bearer 접두 X — 클라이언트가 추가)
 * @param userId      DB PK
 * @param email       사용자 이메일
 * @param displayName 표시 이름
 * @param role        USER / ADMIN
 * @param expiresInSeconds 토큰 만료까지 남은 초 (UI 갱신 용)
 */
public record AuthResponse(
        String token,
        Long userId,
        String email,
        String displayName,
        String role,
        long expiresInSeconds) {
}
