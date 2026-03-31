package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.error.exception.RefreshTokenNotFoundException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class TokenReissueController {

    private final MemberService memberService;

    @PostMapping("/reissue")
    public ResponseEntity<String> reissue(HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new RefreshTokenNotFoundException("Refresh Token이 없습니다.");
        }

        String[] tokens = memberService.reissue(refreshToken);
        String newAccessToken = tokens[0];
        String newRefreshToken = tokens[1];

        Cookie refreshCookie = new Cookie("refreshToken", newRefreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshCookie);

        response.setHeader("Authorization", "Bearer " + newAccessToken);

        return ResponseEntity.ok("토큰이 재발급되었습니다.");
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null)
            return null;

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}