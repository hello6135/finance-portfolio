package com.finance.finportfolio.domain.member.controller;

import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TokenCookieManager {

    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    // 쿠키 생성
    public void setAuthCookies(HttpServletResponse response, String refreshToken) {
        addCookieInternal(response, refreshToken, COOKIE_MAX_AGE);
    }

    // 쿠키 만료
    public void expireAuthCookies(HttpServletResponse response) {
        addCookieInternal(response, "", 0);
    }

    private void addCookieInternal(HttpServletResponse response, String value, int maxAge) {
        String flagValue = (maxAge > 0) ? "true" : "false";

        ResponseCookie refCookie = createCookie("refreshToken", value, maxAge, true);
        ResponseCookie loginFlag = createCookie("isLoggedIn", flagValue, maxAge, false);

        response.addHeader(HttpHeaders.SET_COOKIE, refCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, loginFlag.toString());
    }

    private ResponseCookie createCookie(String name, String value, int maxAge, boolean httpOnly) {
        return ResponseCookie.from(name, value)
                .httpOnly(httpOnly)
                .secure(true) // HTTPS 필수
                .path("/")
                .maxAge(maxAge) // maxAge가 0이면 삭제용, 그 외에는 생성용
                .sameSite("Lax") // CSRF 방지(Lax: 링크 타고 온 건 허용)
                .build();
    }

    // 쿠키 추출
    public String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
