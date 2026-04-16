package com.finance.finportfolio.domain.member.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import com.finance.finportfolio.domain.member.dto.MemberResponseDto;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;

@Component
public class TokenCookieManager {

    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    // 쿠키 생성
    public void setAuthCookies(HttpServletResponse response,
            String refreshToken,
            MemberResponseDto memberResponseDto) {
        addCookieInternal(response, refreshToken, memberResponseDto, COOKIE_MAX_AGE);
    }

    // 쿠키 만료
    public void expireAuthCookies(HttpServletResponse response) {
        addCookieInternal(response, "", null, 0);
    }

    private void addCookieInternal(HttpServletResponse response,
            String refreshToken,
            MemberResponseDto memberResponseDto,
            int maxAge) {

        // 2. 비보안 플래그 쿠키 설정 데이터 준비
        String flagValue = "false";
        String nickname = "";
        String role = "";
        if (maxAge > 0 && memberResponseDto != null) {
            flagValue = "true";
            nickname = memberResponseDto.nickname();
            role = memberResponseDto.role() != null ? memberResponseDto.role().name() : "";
        }

        // 리프레쉬 쿠키(보안)
        ResponseCookie refCookie = createCookie("refreshToken", refreshToken, maxAge, true);
        // 각종 플래그 쿠키(비보안)
        ResponseCookie loginFlag = createCookie("isLoggedIn", flagValue, maxAge, false);
        ResponseCookie nickNameCookie = createCookie("userNickname", nickname, maxAge, false);
        ResponseCookie roleCookie = createCookie("userRole", role, maxAge, false);

        // 리프레쉬 쿠키 헤더에 포함
        response.addHeader(HttpHeaders.SET_COOKIE, refCookie.toString());
        // 플래그 쿠키 헤더에 포함
        response.addHeader(HttpHeaders.SET_COOKIE, loginFlag.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, nickNameCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, roleCookie.toString());
    }

    private ResponseCookie createCookie(@NonNull String name, String value, int maxAge, boolean httpOnly) {
        // 쿠키 값의 Null 체크 및
        String safeValue = (value == null) ? "" : value;

        // 인코딩 처리 (한글 등 Non-ASCII 문자 대응)
        if ("userNickname".equals(name) && value != null) {
            safeValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
                    .replace("\\+", "%20"); // 공백 처리 호환성
        }

        return ResponseCookie.from(name, safeValue)
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
