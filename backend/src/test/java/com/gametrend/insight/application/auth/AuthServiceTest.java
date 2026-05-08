package com.gametrend.insight.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gametrend.insight.domain.user.Role;
import com.gametrend.insight.infrastructure.persistence.user.UserJpaEntity;
import com.gametrend.insight.infrastructure.persistence.user.UserJpaRepository;
import com.gametrend.insight.infrastructure.security.JwtTokenProvider;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserJpaRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepo, passwordEncoder, tokenProvider, 24L);
    }

    @Test
    @DisplayName("register — 이메일 중복 시 EmailAlreadyExistsException")
    void register_duplicateEmail() {
        when(userRepo.existsByEmail("dup@x.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(
                new RegisterRequest("dup@x.com", "password123", "Dup User")))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    @DisplayName("register — 정상 가입 → BCrypt hash 저장 + JWT 발급")
    void register_success() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashed");
        when(userRepo.save(any(UserJpaEntity.class))).thenAnswer(inv -> {
            UserJpaEntity e = inv.getArgument(0);
            setField(e, "id", 100L);
            return e;
        });
        when(tokenProvider.generate(anyLong(), anyString(), anyString())).thenReturn("jwt-token-xyz");

        AuthResponse resp = service.register(new RegisterRequest("new@x.com", "password123", "New User"));

        assertThat(resp.userId()).isEqualTo(100L);
        assertThat(resp.email()).isEqualTo("new@x.com");
        assertThat(resp.token()).isEqualTo("jwt-token-xyz");
        assertThat(resp.role()).isEqualTo("USER");
        assertThat(resp.expiresInSeconds()).isEqualTo(24L * 3600); // 24h
    }

    @Test
    @DisplayName("login — 미존재 이메일 → AuthException 'invalid credentials'")
    void login_unknownEmail() {
        when(userRepo.findByEmail("nope@x.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(new LoginRequest("nope@x.com", "password")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("invalid credentials"); // 보안 — 이메일 누설 X
    }

    @Test
    @DisplayName("login — 비밀번호 불일치 → AuthException ('invalid credentials' 통일)")
    void login_wrongPassword() {
        UserJpaEntity entity = sampleEntity();
        when(userRepo.findByEmail("user@x.com")).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("user@x.com", "wrong")))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("invalid credentials");
    }

    @Test
    @DisplayName("login — 정상 → JWT 발급")
    void login_success() {
        UserJpaEntity entity = sampleEntity();
        when(userRepo.findByEmail("user@x.com")).thenReturn(Optional.of(entity));
        when(passwordEncoder.matches("correct", "$2a$10$hashed")).thenReturn(true);
        when(tokenProvider.generate(1L, "user@x.com", "USER")).thenReturn("valid-jwt");

        AuthResponse resp = service.login(new LoginRequest("user@x.com", "correct"));

        assertThat(resp.token()).isEqualTo("valid-jwt");
        assertThat(resp.userId()).isEqualTo(1L);
    }

    private static UserJpaEntity sampleEntity() {
        try {
            var ctor = UserJpaEntity.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            UserJpaEntity e = ctor.newInstance();
            setField(e, "id", 1L);
            e.setEmail("user@x.com");
            e.setPasswordHash("$2a$10$hashed");
            e.setDisplayName("Test User");
            e.setRole(Role.USER);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void setField(Object o, String name, Object value) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(o, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
