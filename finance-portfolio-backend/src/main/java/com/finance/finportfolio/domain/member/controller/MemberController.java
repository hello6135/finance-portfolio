package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.dto.MemberJoinRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequestDto;
import com.finance.finportfolio.domain.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final TokenCookieManager tokenCookieManager;

    @PostMapping("/join")
    public ResponseEntity<String> join(@Valid @RequestBody MemberJoinRequestDto request) {
        memberService.join(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody MemberLoginRequestDto loginRequest,
            HttpServletResponse response) {

        try {
            // 1. 로그인 처리 → Access Token + Refresh Token 발급
            String[] tokens = memberService.login(loginRequest);
            String accessToken = tokens[0];
            String refreshToken = tokens[1];

            // 2. 토큰 쿠키 전달 (리프레쉬 토큰, 로그인 플래그)
            tokenCookieManager.setAuthCookies(response, refreshToken);

            // 3. Access Token → 응답 헤더로 전달 (프론트에서 authStore 메모리에 저장)
            response.setHeader("Authorization", "Bearer " + accessToken);

            return ResponseEntity.ok("로그인이 완료되었습니다.");
        } catch (Exception e) {
            tokenCookieManager.expireAuthCookies(response);
            throw e; // 기존 예외 처리가 작동하도록 던짐
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        String loginId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        memberService.logout(loginId);

        // 2. 토큰 쿠키 만료 처리 (리프레쉬 토큰, 로그인 플래그)
        tokenCookieManager.expireAuthCookies(response);

        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }

}