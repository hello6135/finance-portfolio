package com.finance.finportfolio.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequestDto;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import({ SecurityConfig.class, TokenCookieManager.class })
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    @MockitoSpyBean
    private TokenCookieManager tokenCookieManager;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("SecurityConfig 빈 정상 로드 확인")
    void securityConfigBeansLoad() {
        assertThat(passwordEncoder).isNotNull();
        assertThat(authenticationManager).isNotNull();
    }

    // ── 회원가입 ───────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("회원가입 성공 시 200과 성공 메시지를 반환한다")
    void join_Success() throws Exception {
        MemberJoinRequestDto request = new MemberJoinRequestDto("testId", "password123", "nickname");

        mockMvc.perform(post("/api/member/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입이 성공적으로 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("아이디가 공백이면 회원가입 요청이 400을 반환한다")
    void join_Fail_BlankLoginId() throws Exception {
        MemberJoinRequestDto request = new MemberJoinRequestDto("", "password123", "nickname");

        mockMvc.perform(post("/api/member/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("비밀번호가 4자 미만이면 회원가입 요청이 400을 반환한다")
    void join_Fail_ShortPassword() throws Exception {
        MemberJoinRequestDto request = new MemberJoinRequestDto("testId", "123", "nickname");

        mockMvc.perform(post("/api/member/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── 로그인 ─────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("로그인 성공 시 Authorization 헤더와 refreshToken 쿠키가 반환된다")
    void login_Success() throws Exception {
        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("testId", "password123");
        given(memberService.login(any())).willReturn(new String[] { "accessToken", "refreshToken" });

        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("로그인이 완료되었습니다."))
                // Access Token 검증
                .andExpect(header().string("Authorization", "Bearer accessToken"))
                // Refresh Token 쿠키 검증 (HttpOnly)
                .andExpect(cookie().value("refreshToken", "refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                // isLoggedIn 플래그 쿠키 검증 (JS 접근 가능)
                .andExpect(cookie().value("isLoggedIn", "true"))
                .andExpect(cookie().httpOnly("isLoggedIn", false));
    }

    @Test
    @WithMockUser
    @DisplayName("JWT 방식에서는 CSRF 없이도 로그인 요청이 성공한다")
    void login_Success_WithoutCsrf() throws Exception {
        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("testId", "password123");
        given(memberService.login(any())).willReturn(new String[] { "accessToken", "refreshToken" });

        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    @DisplayName("아이디가 공백이면 로그인 요청이 400을 반환한다")
    void login_Fail_BlankLoginId() throws Exception {
        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("", "password123");

        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(memberService, never()).login(any());
    }

    // 로그인 try 실패 시 catch

    @Test
    @WithMockUser
    @DisplayName("로그인 실패 시 기존 인증 정보를 지우기 위해 쿠키를 만료시킨다")
    void login_Fail_And_Expire_Cookies() throws Exception {
        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("wrongId", "wrongPw");
        // 서비스에서 예외 발생 시나리오
        given(memberService.login(any())).willThrow(new RuntimeException("로그인 실패"));

        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isInternalServerError()) // 또는 설정한 ExceptionHandler의 응답값
                // 실패 시에도 쿠키 만료 로직이 실행되었는지 확인
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().maxAge("isLoggedIn", 0));
    }

    // ── 로그아웃 ───────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("로그아웃 성공 시 refreshToken 쿠키가 즉시 만료된다")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/member/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃이 완료되었습니다."))
                // 모든 쿠키 maxAge = 0 확인
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().maxAge("isLoggedIn", 0));
    }

}