package com.gametrend.insight.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer 토큰 추출 → JWT 검증 → SecurityContext 설정.
 *
 * <p>토큰 없거나 위조 시 그냥 통과 (anonymous). 권한 필요 endpoint는 SecurityConfig가 401 처리.
 *
 * <p>{@link OncePerRequestFilter} — 같은 요청에 두 번 적용 안 함.
 */
@Component
@ConditionalOnBean(JwtTokenProvider.class)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;

    public JwtAuthFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            try {
                Claims claims = tokenProvider.parse(token);
                Long userId = Long.valueOf(claims.getSubject());
                String email = claims.get("email", String.class);
                String role = claims.get("role", String.class);
                AuthenticatedUser principal = new AuthenticatedUser(userId, email, role);

                var auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                // 통과 — 인증 필요 endpoint는 401 매핑
            }
        }
        chain.doFilter(req, res);
    }
}
