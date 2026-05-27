package com.finance.finportfolio.global.security.filter;

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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String loginId = jwtTokenProvider.getLoginId(token);

                    // DB에서 실시간 정지 여부 검증
                    boolean isBanned = memberRepository.findByLoginId(loginId)
                            .map(Member::isBanned)
                            .orElse(false);

                    if (isBanned) {
                        log.warn("정지된 사용자의 접근 차단: {}", loginId);
                        sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, "BANNED_USER");
                        return; // 필터 체인 중단
                    }

                    // 1. 인증 정보 설정 시 발생할 수 있는 예외 방지
                    setAuthentication(token);
                }
            } catch (ExpiredJwtException e) {
                log.warn("만료된 Access Token: {}", request.getRequestURI());
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "ACCESS_TOKEN_EXPIRED");
                return; // 만료 시 여기서 종료
            } catch (Exception e) {
                // 2. 그 외 모든 예외는 로그를 남기고 인증되지 않은 상태로 진행 (500 에러 방어)
                log.error("JWT 인증 처리 중 오류 발생: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 3. 토큰이 없거나, 유효하지 않거나, 에러가 나더라도 다음 필터로 넘겨야 permitAll이 작동함
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

        if (loginId != null && role != null) {
            // Spring Security의 hasRole()은 기본적으로 "ROLE_" 접두사를 기대합니다.
            String grantedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginId,
                    null,
                    List.of(new SimpleGrantedAuthority(grantedRole)));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    // 예외 발생 시 일관된 JSON 응답을 전송하기 위한 헬퍼 메서드
    private void sendErrorResponse(HttpServletResponse response, int status, String errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", errorCode));
    }
}