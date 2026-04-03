package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.error.GlobalExceptionHandler;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;

@WebMvcTest(TokenReissueController.class)
@Import({ SecurityConfig.class, TokenCookieManager.class })
class TokenReissueControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private MemberService memberService;

        @SpyBean
        private TokenCookieManager tokenCookieManager;

        @MockBean
        private AuthenticationManager authenticationManager;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        // ── 정상 재발급 ────────────────────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("유효한 Refresh Token 쿠키로 요청 시 새 토큰이 발급된다")
        void reissue_Success() throws Exception {
                // given
                String oldRefreshToken = "validOldRefreshToken";
                String newAccessToken = "newAccessToken";
                String newRefreshToken = "newRefreshToken";

                // 1. 추출 로직 Mocking
                org.mockito.Mockito.doReturn(oldRefreshToken)
                                .when(tokenCookieManager).extractRefreshTokenFromCookie(any());

                // 2. 서비스 로직 Mocking
                given(memberService.reissue(oldRefreshToken))
                                .willReturn(new String[] { newAccessToken, newRefreshToken });

                // 3. (중요) 실제 Response에 쿠키를 심어주는 동작 정의
                org.mockito.Mockito.doAnswer(invocation -> {
                        jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(0);
                        String token = invocation.getArgument(1);
                        // 테스트용 간이 쿠키 추가
                        response.addCookie(new Cookie("refreshToken", token));
                        response.addCookie(new Cookie("isLoggedIn", "true"));
                        return null;
                }).when(tokenCookieManager).setAuthCookies(any(), any());

                // when & then
                mockMvc.perform(post("/api/auth/reissue")
                                .cookie(new Cookie("refreshToken", oldRefreshToken)))
                                .andDo(print()) // 이제 출력이 정상적으로 나옵니다.
                                .andExpect(status().isOk())
                                .andExpect(cookie().value("refreshToken", newRefreshToken))
                                .andExpect(cookie().value("isLoggedIn", "true"));
        }

        // ── Refresh Token 없는 경우 ────────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("Refresh Token 쿠키가 없으면 401 에러를 반환한다")
        void reissue_Fail_NoCookie() throws Exception {
                // given
                org.mockito.Mockito.doReturn(null)
                                .when(tokenCookieManager).extractRefreshTokenFromCookie(any());

                // when & then
                mockMvc.perform(post("/api/auth/reissue"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                                .andExpect(jsonPath("$.message", Matchers.containsString("Refresh Token이 없습니다.")));

                verify(memberService, never()).reissue(any());
        }

        // ── 유효하지 않은 Refresh Token ────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("유효하지 않은 Refresh Token이면 400 Bad Request를 반환한다")
        void reissue_Fail_InvalidToken() throws Exception {
                org.mockito.Mockito.doReturn("invalidToken")
                                .when(tokenCookieManager).extractRefreshTokenFromCookie(any());

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
                org.mockito.Mockito.doReturn("stolenToken")
                                .when(tokenCookieManager).extractRefreshTokenFromCookie(any());

                given(memberService.reissue(any()))
                                .willThrow(new IllegalStateException("Refresh Token이 일치하지 않습니다."));

                mockMvc.perform(post("/api/auth/reissue")
                                .cookie(new Cookie("refreshToken", "stolenToken")))
                                .andExpect(status().isBadRequest());
        }

        // -- TokenCookieManager의 extractRefreshTokenFromCookie 메서드 테스트 --

        @Test
        @DisplayName("쿠키 목록에 refreshToken이 있으면 해당 값을 추출한다")
        void extractRefreshToken_Success() {
                // given
                MockHttpServletRequest request = new MockHttpServletRequest();
                Cookie refreshTokenCookie = new Cookie("refreshToken", "test-refresh-token");
                Cookie otherCookie = new Cookie("other", "value");
                request.setCookies(refreshTokenCookie, otherCookie);

                // when
                String result = tokenCookieManager.extractRefreshTokenFromCookie(request);

                // then
                assertThat(result).isEqualTo("test-refresh-token");
        }

        @Test
        @DisplayName("쿠키 목록이 null이면 null을 반환한다")
        void extractRefreshToken_NullCookies() {
                // given
                MockHttpServletRequest request = new MockHttpServletRequest();
                // request.setCookies를 하지 않음 (기본 null)

                // when
                String result = tokenCookieManager.extractRefreshTokenFromCookie(request);

                // then
                assertThat(result).isNull();
        }

        @Test
        @DisplayName("refreshToken이라는 이름의 쿠키가 없으면 null을 반환한다")
        void extractRefreshToken_NoTargetCookie() {
                // given
                MockHttpServletRequest request = new MockHttpServletRequest();
                request.setCookies(new Cookie("accessToken", "some-value"));

                // when
                String result = tokenCookieManager.extractRefreshTokenFromCookie(request);

                // then
                assertThat(result).isNull();
        }
}