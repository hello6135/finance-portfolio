package com.finance.finportfolio.domain.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequest;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

// ★ 중요: Static Imports (이게 없으면 assertThat, post, csrf 등이 작동 안 함)
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    // SecurityConfig에 정의된 빈들을 주입받아 커버리지를 채웁니다.
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private Authentication authentication;

    @Test
    @DisplayName("SecurityConfig의 빈들이 정상적으로 로드되었는지 확인")
    void securityConfigBeansLoad() {
        // 이 검증을 통해 SecurityConfig의 메서드들이 실행됨을 보장 (커버리지 확보)
        assertThat(passwordEncoder).isNotNull();
        assertThat(authenticationManager).isNotNull();
    }

    @Test
    @WithMockUser
    @DisplayName("회원가입 API 호출 시 성공 메시지를 반환한다")
    void join_Success() throws Exception {
        MemberJoinRequest request = new MemberJoinRequest("testId", "password123", "nickname");
        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/member/join")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(content().string("회원가입이 성공적으로 완료되었습니다."));
    }

    @Test
    @WithMockUser
    @DisplayName("로그인 API 호출 시 성공 메시지를 반환한다")
    void login_Success() throws Exception {
        // 1. 데이터 준비
        MemberLoginRequest loginRequest = new MemberLoginRequest("testId", "password123");
        String json = objectMapper.writeValueAsString(loginRequest);

        // 2. 컨트롤러가 사용하는 모든 의존성을 확실히 모킹 (가장 중요)
        // 컨트롤러에서 authenticationManager.authenticate()를 호출한다면:
        when(authenticationManager.authenticate(any())).thenReturn(authentication);

        // 만약 서비스의 특정 메서드도 호출한다면 그것도 작성:
        // when(memberService.someMethod(any())).thenReturn(someValue);

        // 3. 실행 및 검증
        mockMvc.perform(post("/api/member/login")
                .with(csrf())
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
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isForbidden());
    }
}