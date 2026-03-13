package com.finance.finportfolio.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequest;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class) // 컨트롤러만 슬라이스 테스트
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean // 컨트롤러가 의존하는 서비스를 가짜(Mock)로 등록
    private MemberService memberService;

    @Autowired
    private ObjectMapper objectMapper; // 객체를 JSON 문자열로 변환해주는 도구

    @MockBean
    private AuthenticationManager authenticationManager; // 로그인 로직에 필요함

    @MockBean
    private Authentication authentication; // 가짜 인증 결과물

    @Test
    @WithMockUser // 스프링 시큐리티 인증 통과를 위한 가짜 유저
    @DisplayName("회원가입 API 호출 시 성공 메시지를 반환한다")
    void join_Success() throws Exception {
        // given
        MemberJoinRequest request = new MemberJoinRequest("testId", "password123", "nickname");
        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/member/join")
                .with(csrf()) // 시큐리티 사용 시 CSRF 토큰 필요 (테스트용)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입이 성공적으로 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("로그인 API 호출 시 성공 메시지를 반환한다")
    void login_Success() throws Exception {
        // Given: 로그인 요청 데이터 준비
        MemberLoginRequest loginRequest = new MemberLoginRequest("testId", "password123");
        String json = objectMapper.writeValueAsString(loginRequest);

        // Stubbing: 인증 매니저가 성공적인 인증 결과(authentication)를 반환한다고 가정
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        // When & Then: 호출 및 결과 확인
        mockMvc.perform(post("/api/member/login")
                .with(csrf()) // Security 적용 시 필수
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("로그인이 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("CSRF 토큰 없이 POST 요청 시 403 Forbidden 에러가 발생해야 한다")
    void login_Fail_Without_Csrf() throws Exception {
        MemberLoginRequest loginRequest = new MemberLoginRequest("testId", "password123");
        String json = objectMapper.writeValueAsString(loginRequest);

        mockMvc.perform(post("/api/member/login")
                // .with(csrf()) 를 고의로 누락
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden()); // 403 응답 확인
    }
}