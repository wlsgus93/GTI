package com.gametrend.insight.application.auth;

import com.gametrend.insight.domain.user.Role;
import com.gametrend.insight.domain.user.User;
import com.gametrend.insight.infrastructure.persistence.user.UserJpaEntity;
import com.gametrend.insight.infrastructure.persistence.user.UserJpaRepository;
import com.gametrend.insight.infrastructure.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 — 회원가입 + 로그인 + JWT 발급.
 *
 * <p>보안 결정:
 * <ul>
 *   <li>비밀번호 BCrypt (Spring Security 기본, cost=10)
 *   <li>로그인 실패 메시지 통일 ("invalid credentials") — 이메일 존재 여부 누설 차단
 *   <li>토큰 발급 시 password_hash 노출 X (응답에 포함 안 함)
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserJpaRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long expirationHours;

    public AuthService(
            UserJpaRepository userRepo,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("${gti.jwt.expiration-hours}") long expirationHours) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.expirationHours = expirationHours;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.email())) {
            throw new EmailAlreadyExistsException(req.email());
        }

        String hash = passwordEncoder.encode(req.password());
        User domain = new User(null, req.email(), hash, req.displayName(), Role.USER, null);
        UserJpaEntity saved = userRepo.save(UserJpaEntity.from(domain));
        log.info("New user registered: id={}, email={}", saved.getId(), saved.getEmail());

        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        UserJpaEntity user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new AuthException("invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthException("invalid credentials");
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(UserJpaEntity user) {
        String token = tokenProvider.generate(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                expirationHours * 3600);
    }
}
