package com.finance.finportfolio.global.config;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
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

import com.finance.finportfolio.global.security.filter.JwtAuthenticationFilter;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SuppressWarnings("java:S4502")
public class SecurityConfig {

        @Value("${cloudfront.custom.header.name}")
        private String cfHeaderName;

        private final JwtTokenProvider jwtTokenProvider;

        // 중복방지용 상수처리
        public static final String USER = "USER";
        public static final String ADMIN = "ADMIN";

        // 기본 Rounds 10(실무 표준은 10~12, 복잡도는 1상승 시 2배 씩 증가)
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
                                                // 예비요청(OPTIONS)
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                // 에러페이지
                                                .requestMatchers("/error").permitAll()
                                                // 게시판
                                                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()
                                                .requestMatchers("/api/posts/**").hasAnyRole(USER, ADMIN)
                                                .requestMatchers(HttpMethod.POST, "/api/image/upload")
                                                .hasAnyRole(USER, ADMIN)
                                                // 게시판 카테고리(관리는 ADMIN 제한)
                                                .requestMatchers(HttpMethod.GET, "/api/category/**").permitAll()
                                                .requestMatchers("/api/category/**").hasRole(ADMIN)
                                                // 회원관리
                                                .requestMatchers("/api/member/join", "/api/member/login",
                                                                "/api/member/reissue")
                                                .permitAll()
                                                .requestMatchers("/api/member/logout").hasAnyRole(USER, ADMIN)
                                                // 금융 계산기 등
                                                .requestMatchers("/api/finance/**").permitAll()
                                                // 관리자 기능
                                                .requestMatchers("/api/admin/**").hasRole(ADMIN)
                                                // 명시되지 않은 경로 모두 차단
                                                .anyRequest().denyAll())

                                // 인증되지 않은 사용자가 보호된 리소스에 접근 시 응답 설정
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter().write("{\"error\": \"UNAUTHORIZED\"}");
                                                })
                                                .accessDeniedHandler((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.setContentType("application/json;charset=UTF-8");
                                                        response.getWriter().write("{\"error\": \"FORBIDDEN\"}");
                                                }))

                                // ── JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 등록 ──
                                .addFilterBefore(jwtAuthenticationFilter(),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // CloudFront 통합 관리로 자기 경로 참조하기 때문에 Origin 허용 필수는 아님
                configuration.setAllowedOrigins(List.of(
                                "http://localhost:3000",
                                "https://www.ljh-finance.com",
                                "https://dev.ljh-finance.com"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of(
                                "Authorization", // JWT 토큰용
                                "Content-Type", // JSON 데이터 전송용
                                "X-Requested-With", // AJAX 요청 식별용
                                cfHeaderName // CloudFront 커스텀헤더 (EC2 접근용)
                ));
                configuration.setAllowCredentials(true);
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용
                return source;
        }
}