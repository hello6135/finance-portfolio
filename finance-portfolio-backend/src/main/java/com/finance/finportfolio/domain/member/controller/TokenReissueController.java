package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.dto.LoginResultDto;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.error.exception.RefreshTokenNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class TokenReissueController {

    private final MemberService memberService;
    private final TokenCookieManager tokenCookieManager;

    // 토큰 재발급
    @PostMapping("/reissue")
    public ResponseEntity<String> reissue(HttpServletRequest request,
            HttpServletResponse response) {

        // 쿠키 추출
        String refreshToken = tokenCookieManager.extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            // 혹시 모르니 토큰 즉시 만료
            tokenCookieManager.expireAuthCookies(response);
            throw new RefreshTokenNotFoundException("Refresh Token이 없습니다.");
        }

        try {
            LoginResultDto loginResult = memberService.reissue(refreshToken);

            tokenCookieManager.setAuthCookies(response, loginResult.refreshToken(), loginResult.memberResponseDto());

            response.setHeader("Authorization", "Bearer " + loginResult.accessToken());

            return ResponseEntity.ok("토큰이 재발급되었습니다.");
        } catch (Exception e) {
            tokenCookieManager.expireAuthCookies(response);
            throw e; // 기존 예외 처리가 작동하도록 던짐
        }
    }

}