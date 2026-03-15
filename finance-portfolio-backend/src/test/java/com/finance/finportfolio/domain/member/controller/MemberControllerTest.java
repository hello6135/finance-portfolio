package com.finance.finportfolio.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequest;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@Import(SecurityConfig.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MemberService memberService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtTokenProvider jwtTokenProvider; // ← SecurityConfig 주입용, 추가 필수

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("SecurityConfig 빈 정상 로드 확인")
    void securityConfigBeansLoad() {
        assertThat(passwordEncoder).isNotNull();
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    @WithMockUser
    @DisplayName("회원가입 API 호출 시 성공 메시지를 반환한다")
    void join_Success() throws Exception {
        MemberJoinRequest request = new MemberJoinRequest("testId", "password123", "nickname");

        mockMvc.perform(post("/api/member/join")
                // csrf() 제거 — JWT 방식은 CSRF 비활성화
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입이 성공적으로 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("로그인 API 호출 시 성공 메시지를 반환한다")
    void login_Success() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("testId", "password123");

        // memberService.login()이 토큰 배열 반환하도록 모킹
        when(memberService.login(any())).thenReturn(new String[] { "accessToken", "refreshToken" });

        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("로그인이 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("JWT 방식에서는 CSRF 없이도 POST 요청이 성공해야 한다")
    void login_Success_Without_Csrf() throws Exception {
        // JWT로 전환 후 CSRF 비활성화 → csrf() 없어도 200 반환되어야 함
        MemberLoginRequest loginRequest = new MemberLoginRequest("testId", "password123");
        when(memberService.login(any())).thenReturn(new String[] { "accessToken", "refreshToken" });

        mockMvc.perform(post("/api/member/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }
}