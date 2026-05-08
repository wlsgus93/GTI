package com.gametrend.insight.infrastructure.security;

/**
 * SecurityContextHolder.principal로 노출되는 인증된 사용자 정보.
 *
 * <p>JwtAuthFilter가 토큰 파싱 후 {@code Authentication} 의 principal로 설정.
 * Controller에서 {@code @AuthenticationPrincipal AuthenticatedUser} 로 주입.
 */
public record AuthenticatedUser(Long id, String email, String role) {
}
