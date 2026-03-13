package com.finance.finportfolio.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 비밀번호 암호화 (BCrypt 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 컨트롤러에 주입해주기 위해 Bean 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @SuppressWarnings("java:S3330")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 방지
                // NOSONAR: React에서 CSRF 토큰을 읽기 위해 HttpOnly(false)가 필수적임
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) // React가 쿠키를 읽을 수 있게 설정
                )
                // 추가적인 보안 헤더 설정 - XSS 방어
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; " + // 모든 리소스는 동일 출처(자기 자신)만 허용
                                        "script-src 'self'; " + // 스크립트 실행도 자기 자신만 허용 (인라인 스크립트 차단)
                                        "style-src 'self' 'unsafe-inline'; " + // CSS는 인라인 스타일 허용 (React 스타일링 대응)
                                        "img-src 'self' data: https://*.s3.amazonaws.com; " + // S3 이미지 로딩 허용
                                        "connect-src 'self';") // API 통신은 자기 자신과만 가능
                        )) // 세션 설정 (`IF_REQUIRED`: 요청 시)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                // 인가 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/member/join", "/api/member/login").permitAll() // 가입, 로그인은 모두 허용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()); // 그 외 모든 요청 인증 필요
        return http.build();
    }
}
