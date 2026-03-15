package com.finance.finportfolio.global.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // HS256 최소 32바이트 → Base64 인코딩 필요
    private static final String SECRET = "dGVzdC1zZWNyZXQta2V5LW11c3QtYmUtYXQtbGVhc3QtMzItY2hhcmFjdGVycw==";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 900000L); // 15분
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L); // 7일
    }

    // ── Access Token ───────────────────────────────────────────

    @Test
    @DisplayName("Access Token 생성 후 loginId와 role을 정상적으로 추출할 수 있다")
    void createAccessToken_Success() {
        String token = jwtTokenProvider.createAccessToken("testUser", "ROLE_USER");

        assertThat(jwtTokenProvider.getLoginId(token)).isEqualTo("testUser");
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("유효한 Access Token은 validateToken이 true를 반환한다")
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtTokenProvider.createAccessToken("testUser", "ROLE_USER");

        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("위변조된 토큰은 validateToken이 false를 반환한다")
    void validateToken_TamperedToken_ReturnsFalse() {
        String token = jwtTokenProvider.createAccessToken("testUser", "ROLE_USER");
        String tamperedToken = token + "tampered";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("만료된 Access Token은 validateToken에서 ExpiredJwtException을 던진다")
    void validateToken_ExpiredToken_ThrowsExpiredJwtException() {
        // 만료 시간을 -1초로 설정 → 즉시 만료
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", -1000L);
        String expiredToken = jwtTokenProvider.createAccessToken("testUser", "ROLE_USER");

        assertThatThrownBy(() -> jwtTokenProvider.validateToken(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ── Refresh Token ──────────────────────────────────────────

    @Test
    @DisplayName("Refresh Token 생성 후 loginId를 정상적으로 추출할 수 있다")
    void createRefreshToken_Success() {
        String token = jwtTokenProvider.createRefreshToken("testUser");

        assertThat(jwtTokenProvider.getLoginId(token)).isEqualTo("testUser");
    }

    @Test
    @DisplayName("유효한 Refresh Token은 isTokenExpired가 false를 반환한다")
    void isTokenExpired_ValidToken_ReturnsFalse() {
        String token = jwtTokenProvider.createRefreshToken("testUser");

        assertThat(jwtTokenProvider.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("만료된 Refresh Token은 isTokenExpired가 true를 반환한다")
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", -1000L);
        String expiredToken = jwtTokenProvider.createRefreshToken("testUser");

        assertThat(jwtTokenProvider.isTokenExpired(expiredToken)).isTrue();
    }

    // ── 기타 ───────────────────────────────────────────────────

    @Test
    @DisplayName("getRefreshTokenExpiration은 설정값을 반환한다")
    void getRefreshTokenExpiration_ReturnsConfiguredValue() {
        assertThat(jwtTokenProvider.getRefreshTokenExpiration()).isEqualTo(604800000L);
    }

    @Test
    @DisplayName("완전히 잘못된 형식의 토큰은 validateToken이 false를 반환한다")
    void validateToken_InvalidFormat_ReturnsFalse() {
        assertThat(jwtTokenProvider.validateToken("this.is.not.a.jwt")).isFalse();
    }
}