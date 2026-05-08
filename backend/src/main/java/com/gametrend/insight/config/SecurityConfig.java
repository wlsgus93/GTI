package com.gametrend.insight.config;

import com.gametrend.insight.infrastructure.security.JwtAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * W4 D4 보안 설정 — JWT 도입 (W1 임시 permitAll에서 점진 교체).
 *
 * <p>인증 정책:
 * <ul>
 *   <li>{@code /api/v1/auth/**} — permitAll (회원가입/로그인)
 *   <li>{@code /api/v1/watchlist/**} — authenticated (사용자 데이터)
 *   <li>그 외 GET (trends/games/dimensions 등) — permitAll (W4 시점 — 게스트 보기 허용)
 *   <li>그 외 POST/PUT/DELETE — 일단 permitAll 유지 (verification/moneycalc 등 — W4 후속에 인증 강화)
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, ObjectProvider<JwtAuthFilter> jwtFilterProvider) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/watchlist/**").authenticated()
                        .requestMatchers("/api/v1/agent/**").authenticated()
                        .anyRequest().permitAll());

        // ObjectProvider — @WebMvcTest 슬라이스에서 JwtAuthFilter 빈이 없어도 SecurityConfig 로드 가능.
        // 풀 컨텍스트(SpringBootTest 또는 운영)에선 필터 자동 등록.
        JwtAuthFilter jwtFilter = jwtFilterProvider.getIfAvailable();
        if (jwtFilter != null) {
            http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
