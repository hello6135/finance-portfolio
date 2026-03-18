package com.finance.finportfolio.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.finance.finportfolio.global.security.JwtAuthenticationFilter;
import com.finance.finportfolio.global.security.JwtTokenProvider;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SuppressWarnings("java:S4502")
public class SecurityConfig {

        private final JwtTokenProvider jwtTokenProvider;

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
                        throws Exception {
                return authConfig.getAuthenticationManager();
        }

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter() {
                return new JwtAuthenticationFilter(jwtTokenProvider);
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // ── CSRF: JWT 방식은 세션 미사용 → CSRF 불필요 ──────────
                                .csrf(csrf -> csrf.disable())

                                // ── CORS: S3/CloudFront 도메인 허용 ─────────────────────
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                                // ── 보안 헤더 (기존 CSP 유지) ────────────────────────────
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives("default-src 'self'; " +
                                                                                "script-src 'self'; " +
                                                                                "style-src 'self' 'unsafe-inline'; " +
                                                                                "img-src 'self' data: https://*.s3.amazonaws.com; "
                                                                                +
                                                                                "connect-src 'self';")))

                                // ── 세션 STATELESS: JWT 방식은 서버에 세션 저장 안 함 ────
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // ── 인가 설정 ────────────────────────────────────────────
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                                                .requestMatchers("/api/member/join", "/api/member/login").permitAll()
                                                .requestMatchers("/api/auth/reissue").permitAll()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .anyRequest().authenticated())

                                // ── JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록 ──
                                .addFilterBefore(jwtAuthenticationFilter(),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // ── CORS 설정: React(S3/CloudFront) 도메인 허용 ───────────────
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                config.setAllowedOrigins(List.of(
                                "http://localhost:3000", // 로컬 개발 (Vite 기본 포트)
                                "https://www.ljh-finance.com", // 실제 CloudFront 도메인
                                "https://dev.ljh-finance.com"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));

                // Authorization 헤더를 프론트에서 읽을 수 있도록 허용
                config.setExposedHeaders(List.of("Authorization"));
                config.setAllowCredentials(true); // Refresh Token 쿠키 전달 허용
                config.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}