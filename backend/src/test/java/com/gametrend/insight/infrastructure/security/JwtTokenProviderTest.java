package com.gametrend.insight.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET_32 = "test-secret-must-be-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "completely-different-secret-32-bytes-long-zzz";

    @Test
    @DisplayName("정상 발급 + 파싱 — subject/email/role 추출")
    void generateAndParse() {
        var provider = new JwtTokenProvider(SECRET_32, 24, "test-issuer");
        String token = provider.generate(42L, "user@example.com", "USER");

        Claims claims = provider.parse(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuer()).isEqualTo("test-issuer");
    }

    @Test
    @DisplayName("32 byte 미만 secret → 부팅 차단")
    void shortSecret_rejected() {
        assertThatThrownBy(() -> new JwtTokenProvider("short", 24, "test-issuer"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("다른 secret으로 발급한 토큰 → SignatureException")
    void wrongSecret_rejected() {
        var provider = new JwtTokenProvider(SECRET_32, 24, "test-issuer");
        var attacker = new JwtTokenProvider(OTHER_SECRET, 24, "test-issuer");
        String forged = attacker.generate(99L, "evil@example.com", "ADMIN");

        assertThatThrownBy(() -> provider.parse(forged))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("만료된 토큰 → ExpiredJwtException")
    void expired_rejected() throws InterruptedException {
        // expiration -1 hours = 이미 만료 (음수 허용 X → 0으로 강제하면 즉시 만료)
        // jjwt는 만료 시점이 issuedAt+0이면 즉각 만료. 1ms 차이도 인식.
        // 0 hours 통한 즉시 만료 가능하지만 더 안전한 방법: provider.generate 후 Thread.sleep(...)
        // 여기서는 0 hours로 검증
        // 실제로는 0 시간 = 이미 만료된 토큰
        var provider = new JwtTokenProvider(SECRET_32, 0, "test-issuer");
        String token = provider.generate(1L, "u", "USER");
        Thread.sleep(1100); // expiration이 issuedAt + 0h 이므로 즉시 만료지만 jjwt 정확도 위해 짧은 대기

        assertThatThrownBy(() -> provider.parse(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("잘못된 issuer → MissingClaimException 또는 IncorrectClaim")
    void wrongIssuer_rejected() {
        var providerA = new JwtTokenProvider(SECRET_32, 24, "issuer-A");
        var providerB = new JwtTokenProvider(SECRET_32, 24, "issuer-B");
        String token = providerA.generate(1L, "u", "USER");

        assertThatThrownBy(() -> providerB.parse(token))
                .isInstanceOf(io.jsonwebtoken.IncorrectClaimException.class);
    }
}
