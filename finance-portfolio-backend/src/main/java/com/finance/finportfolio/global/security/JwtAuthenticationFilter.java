package com.finance.finportfolio.global.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        // 토큰 없으면 다음 필터로 (인증 불필요한 엔드포인트는 SecurityConfig에서 처리)
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtTokenProvider.validateToken(token)) {
                setAuthentication(token);
            }
        } catch (ExpiredJwtException e) {
            // 만료된 토큰 → 401 반환 (프론트에서 /reissue 요청하도록 유도)
            log.warn("만료된 Access Token: {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\": \"ACCESS_TOKEN_EXPIRED\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ── Authorization 헤더에서 Bearer 토큰 추출 ────────────────
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // ── SecurityContext에 인증 정보 저장 ───────────────────────
    private void setAuthentication(String token) {
        String loginId = jwtTokenProvider.getLoginId(token);
        String role = jwtTokenProvider.getRole(token);

        // DB 조회 없이 토큰 클레임만으로 인증 객체 생성 → 성능 최적화
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                loginId,
                null,
                List.of(new SimpleGrantedAuthority(role)));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}