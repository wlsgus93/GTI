package com.gametrend.insight.presentation.auth;

import com.gametrend.insight.application.auth.AuthResponse;
import com.gametrend.insight.application.auth.AuthService;
import com.gametrend.insight.application.auth.LoginRequest;
import com.gametrend.insight.application.auth.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST. JWT 발급 — Bearer 헤더로 후속 호출에 부착.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "회원가입 + 로그인 + JWT")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "회원가입 → JWT 즉시 발급", description = "성공 시 토큰 + 사용자 정보 반환")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return service.register(req);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인 → JWT 발급",
            description = "실패 메시지는 이메일 존재 여부와 무관하게 통일 — 보안")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }
}
