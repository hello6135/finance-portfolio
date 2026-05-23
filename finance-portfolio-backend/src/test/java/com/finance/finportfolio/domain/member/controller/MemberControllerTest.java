package com.finance.finportfolio.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.member.dto.LoginResultDto;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberResponseDto;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.security.filter.IpRateLimitFilter;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
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

    @MockitoBean
    private IpRateLimitFilter ipRateLimitFilter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // IpRateLimitFilter Mock 설정: 항상 체인을 통과시킴
    @org.junit.jupiter.api.BeforeEach
    void bypassRateLimitFilter() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(ipRateLimitFilter).doFilter(any(), any(), any());
    }

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
    @DisplayName("[Local 정책] isSecure가 false일 때 쿠키 설정 검증")
    void login_Success_Local_Policy() throws Exception {
        // given: 리플렉션으로 private 필드 강제 수정
        ReflectionTestUtils.setField(tokenCookieManager, "isSecure", false);

        String nickname = "테스터";
        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("testId", "password123");
        MemberResponseDto memberResponseDto = new MemberResponseDto(nickname, Role.USER);
        LoginResultDto loginResult = new LoginResultDto("accessToken", "refreshToken", memberResponseDto);

        given(memberService.login(any(MemberLoginRequestDto.class))).willReturn(loginResult);

        // when & then
        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().secure("refreshToken", false))
                .andExpect(cookie().attribute("refreshToken", "SameSite", "Strict"));
    }

    @Test
    @WithMockUser
    @DisplayName("[Prod 정책] isSecure가 true일 때 쿠키 설정 검증")
    void login_Success_Prod_Policy() throws Exception {
        // given: 리플렉션으로 private 필드 강제 수정
        ReflectionTestUtils.setField(tokenCookieManager, "isSecure", true);

        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("testId", "password123");
        MemberResponseDto memberResponseDto = new MemberResponseDto("테스터", Role.USER);
        LoginResultDto loginResult = new LoginResultDto("accessToken", "refreshToken", memberResponseDto);

        given(memberService.login(any(MemberLoginRequestDto.class))).willReturn(loginResult);

        // when & then
        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().secure("refreshToken", true))
                .andExpect(cookie().attribute("refreshToken", "SameSite", "Strict"));
    }

    @Test
    @WithMockUser
    @DisplayName("JWT 방식(Stateless)이므로 CSRF 토큰 없이도 로그인 요청이 성공한다")
    void login_Success_WithoutCsrf() throws Exception {
        // given
        MemberLoginRequestDto loginRequest = new MemberLoginRequestDto("testId", "password123");
        MemberResponseDto memberResponseDto = new MemberResponseDto("테스터", Role.USER);
        // 서비스 응답 객체 생성 (기존 String[]에서 LoginResultDto로 변경)
        LoginResultDto loginResult = new LoginResultDto("accessToken", "refreshToken", memberResponseDto);

        given(memberService.login(any(MemberLoginRequestDto.class))).willReturn(loginResult);

        // when & then
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