package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.dto.LoginResultDto;
import com.finance.finportfolio.domain.member.dto.MemberResponseDto;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.assertj.core.api.Assertions.assertThat;

import org.hamcrest.Matchers;

@WebMvcTest(TokenReissueController.class)
@Import({ SecurityConfig.class, TokenCookieManager.class })
class TokenReissueControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private MemberService memberService;

        @MockitoSpyBean
        private TokenCookieManager tokenCookieManager;

        @MockitoBean
        private AuthenticationManager authenticationManager;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

        // ── 정상 재발급 ────────────────────────────────────────────

        @Test
        @WithMockUser
        @DisplayName("유효한 Refresh Token 쿠키로 요청 시 새 토큰과 플래그 쿠키가 발급된다")
        void reissue_Success() throws Exception {
                // given
                String oldRefreshToken = "validOldRefreshToken";
                String newAccessToken = "newAccessToken";
                String newRefreshToken = "newRefreshToken";
                String rawNickname = "테스터"; // 원본 닉네임

                // 검증 시 사용할 인코딩된 닉네임 미리 정의
                String encodedNickname = java.net.URLEncoder.encode(rawNickname,
                                java.nio.charset.StandardCharsets.UTF_8);

                MemberResponseDto memberResponseDto = new MemberResponseDto(rawNickname, Role.USER);

                // 서비스 응답 객체 (LoginResultDto)
                LoginResultDto loginResult = new LoginResultDto(newAccessToken, newRefreshToken, memberResponseDto);

                // 1. 쿠키 추출 로직 Mocking
                org.mockito.Mockito.doReturn(oldRefreshToken)
                                .when(tokenCookieManager)
                                .extractRefreshTokenFromCookie(any());

                // 2. 서비스 로직 Mocking (새로운 DTO 반환 타입 적용)
                given(memberService.reissue(oldRefreshToken)).willReturn(loginResult);

                // 3. TokenCookieManager의 setAuthCookies 동작 정의
                // 파라미터: (response, refreshToken, memberResponseDto) - 총 3개
                org.mockito.Mockito.doAnswer(invocation -> {
                        jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(0);
                        String token = invocation.getArgument(1);
                        MemberResponseDto dto = invocation.getArgument(2);

                        // 실제 로직과 유사하게 테스트용 쿠키 주입
                        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                                        "refreshToken=" + token + "; Secure; HttpOnly; Path=/");
                        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                                        "userNickname=" + encodedNickname + "; Secure; Path=/");
                        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                                        "isLoggedIn=true; Secure; Path=/");
                        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE,
                                        "userRole=" + dto.role().name() + "; Secure; Path=/");
                        return null;
                }).when(tokenCookieManager).setAuthCookies(any(), any(), any());

                // when & then
                mockMvc.perform(post("/api/member/reissue") // 엔드포인트 경로 확인 필요
                                .cookie(new Cookie("refreshToken", oldRefreshToken)))
                                .andExpect(status().isOk())
                                // Access Token 헤더 검증
                                .andExpect(header().string("Authorization", "Bearer " + newAccessToken))
                                // Refresh Token 및 플래그 쿠키 검증
                                .andExpect(cookie().value("refreshToken", newRefreshToken))
                                .andDo(print())
                                .andExpect(cookie().value("isLoggedIn", "true"))
                                .andExpect(cookie().value("userNickname", encodedNickname))
                                .andExpect(cookie().value("userRole", "USER"));
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
                mockMvc.perform(post("/api/member/reissue"))
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

                mockMvc.perform(post("/api/member/reissue")
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

                mockMvc.perform(post("/api/member/reissue")
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