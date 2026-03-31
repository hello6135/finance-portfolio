package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenReissueController.class)
@Import(SecurityConfig.class)
class TokenReissueControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MemberService memberService;

        @MockBean
        private AuthenticationManager authenticationManager;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        // ── 정상 재발급 ────────────────────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("유효한 Refresh Token 쿠키로 요청 시 새 토큰이 발급된다")
        void reissue_Success() throws Exception {
                given(memberService.reissue(any()))
                                .willReturn(new String[] { "newAccessToken", "newRefreshToken" });

                mockMvc.perform(post("/api/auth/reissue")
                                .cookie(new Cookie("refreshToken", "validRefreshToken")))
                                .andExpect(status().isOk())
                                .andExpect(content().string("토큰이 재발급되었습니다."))
                                // 새 Access Token이 Authorization 헤더에 담겨야 함
                                .andExpect(header().string("Authorization", "Bearer newAccessToken"))
                                // 새 Refresh Token이 HttpOnly 쿠키로 내려와야 함
                                .andExpect(cookie().value("refreshToken", "newRefreshToken"))
                                .andExpect(cookie().httpOnly("refreshToken", true));
        }

        // ── Refresh Token 없는 경우 ────────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("Refresh Token 쿠키가 없으면 401과 함께 에러 JSON을 반환한다")
        void reissue_Fail_NoCookie() throws Exception {
                mockMvc.perform(post("/api/auth/reissue"))
                                .andExpect(status().isUnauthorized())
                                // 1. JSON 응답의 status 필드 확인
                                .andExpect(jsonPath("$.status").value(401))
                                // 2. 정의한 에러 코드(REFRESH_TOKEN_NOT_FOUND) 확인
                                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                                // 3. 메시지 내용 확인 (resolveMessage가 작동하므로 포함 여부로 확인하는 게 안전)
                                .andExpect(jsonPath("$.message")
                                                .value(org.hamcrest.Matchers.containsString("Refresh Token이 없습니다.")));

                // 쿠키 없으면 서비스 호출 안 해야 함 (검증 로직은 그대로 유지)
                verify(memberService, never()).reissue(any());
        }

        // ── 유효하지 않은 Refresh Token ────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("유효하지 않은 Refresh Token이면 400 Bad Request를 반환한다")
        void reissue_Fail_InvalidToken() throws Exception {
                given(memberService.reissue(any()))
                                .willThrow(new IllegalStateException("유효하지 않은 Refresh Token입니다."));

                mockMvc.perform(post("/api/auth/reissue")
                                .cookie(new Cookie("refreshToken", "invalidToken")))
                                .andExpect(status().isBadRequest());
        }

        // ── 탈취 감지 ──────────────────────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("탈취 감지 시 (토큰 불일치) 400 Bad Request를 반환한다")
        void reissue_Fail_TokenMismatch() throws Exception {
                given(memberService.reissue(any()))
                                .willThrow(new IllegalStateException("Refresh Token이 일치하지 않습니다."));

                mockMvc.perform(post("/api/auth/reissue")
                                .cookie(new Cookie("refreshToken", "stolenToken")))
                                .andExpect(status().isBadRequest());
        }
}