package com.gametrend.insight.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 발급 + 검증 — W4 D4.
 *
 * <p>운영 주의:
 * <ul>
 *   <li>secret은 32 byte (256 bit) 이상 — application.yml에서 환경변수
 *   <li>HS256 — 단일 서비스 충분. 멀티 서비스라면 RS256 (RSA)
 *   <li>refresh token 미구현 — W4 후속
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;

    public JwtTokenProvider(
            @Value("${gti.jwt.secret}") String secret,
            @Value("${gti.jwt.expiration-hours}") long expirationHours,
            @Value("${gti.jwt.issuer}") String issuer) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must be ≥ 32 bytes (256 bit). Set gti.jwt.secret env var.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiration = Duration.ofHours(expirationHours);
        this.issuer = issuer;
    }

    /**
     * @param userId   user PK — subject
     * @param email    snapshot (만료 전 변경 가능, 권한 검증엔 X)
     * @param role     'USER' / 'ADMIN'
     */
    public String generate(long userId, String email, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * @return claims (subject, email, role 포함). 만료/위조 시 예외.
     * @throws io.jsonwebtoken.JwtException 만료/서명 위조/형식 오류
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
