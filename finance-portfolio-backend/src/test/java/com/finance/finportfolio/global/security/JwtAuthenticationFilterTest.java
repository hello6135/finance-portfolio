package com.finance.finportfolio.global.security;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        // 각 테스트 전 SecurityContext 초기화
        SecurityContextHolder.clearContext();
    }

    // ── 정상 인증 ──────────────────────────────────────────────

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 정보가 저장된다")
    void doFilter_ValidToken_SetsAuthentication() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer validToken");

        given(jwtTokenProvider.validateToken("validToken")).willReturn(true);
        given(jwtTokenProvider.getLoginId("validToken")).willReturn("testUser");
        given(jwtTokenProvider.getRole("validToken")).willReturn("ROLE_USER");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // SecurityContext에 인증 정보 저장 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("testUser");

        // 다음 필터로 정상 진행 확인
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ── 토큰 없는 경우 ─────────────────────────────────────────

    @Test
    @DisplayName("Authorization 헤더가 없으면 SecurityContext에 인증 정보가 없고 다음 필터로 진행한다")
    void doFilter_NoToken_PassesThrough() throws ServletException, IOException {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 인증 정보 없음 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 다음 필터로 정상 진행 확인
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 접두사가 없는 토큰은 무시하고 다음 필터로 진행한다")
    void doFilter_NoBearerPrefix_PassesThrough() throws ServletException, IOException {
        request.addHeader("Authorization", "InvalidTokenFormat");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);

        // 헤더 형식이 잘못되면 토큰 검증 자체를 호출하지 않아야 함
        verify(jwtTokenProvider, never()).validateToken(any());
    }

    // ── 만료된 토큰 ────────────────────────────────────────────

    @Test
    @DisplayName("만료된 토큰이면 401과 ACCESS_TOKEN_EXPIRED 에러를 반환한다")
    void doFilter_ExpiredToken_Returns401() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer expiredToken");

        given(jwtTokenProvider.validateToken("expiredToken"))
                .willThrow(new ExpiredJwtException(null, null, "Token expired"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 401 반환 확인
        assertThat(response.getStatus()).isEqualTo(401);

        // 응답 바디에 ACCESS_TOKEN_EXPIRED 포함 확인
        assertThat(response.getContentAsString()).contains("ACCESS_TOKEN_EXPIRED");

        // 만료된 경우 다음 필터로 진행하면 안 됨
        verify(filterChain, never()).doFilter(request, response);
    }

    // ── 위변조된 토큰 ──────────────────────────────────────────

    @Test
    @DisplayName("위변조된 토큰이면 SecurityContext에 인증 정보가 없고 다음 필터로 진행한다")
    void doFilter_InvalidToken_NoAuthentication() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer tamperedToken");

        given(jwtTokenProvider.validateToken("tamperedToken")).willReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // 인증 정보 없음 확인
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        // 위변조 토큰은 그냥 통과 (이후 인증 필요한 엔드포인트에서 403 처리)
        verify(filterChain, times(1)).doFilter(request, response);
    }

    // ── role 검증 ──────────────────────────────────────────────

    @Test
    @DisplayName("ADMIN 토큰이면 SecurityContext에 ROLE_ADMIN 권한이 저장된다")
    void doFilter_AdminToken_SetsAdminRole() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer adminToken");

        given(jwtTokenProvider.validateToken("adminToken")).willReturn(true);
        given(jwtTokenProvider.getLoginId("adminToken")).willReturn("adminUser");
        given(jwtTokenProvider.getRole("adminToken")).willReturn("ROLE_ADMIN");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");
    }
}