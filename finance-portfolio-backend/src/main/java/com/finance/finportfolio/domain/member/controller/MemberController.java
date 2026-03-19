package com.finance.finportfolio.domain.member.controller;

import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequest;
import com.finance.finportfolio.domain.member.service.MemberService;
import jakarta.servlet.http.Cookie;
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

    @PostMapping("/join")
    public ResponseEntity<String> join(@Valid @RequestBody MemberJoinRequest request) {
        memberService.join(request);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody MemberLoginRequest loginRequest,
            HttpServletResponse response) {

        // 1. 로그인 처리 → Access Token + Refresh Token 발급
        String[] tokens = memberService.login(loginRequest);
        String accessToken = tokens[0];
        String refreshToken = tokens[1];

        // 2. Refresh Token → HttpOnly 쿠키로 전달 (JS 접근 불가 → XSS 방어)
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true); // JS에서 접근 불가
        refreshCookie.setSecure(true); // HTTPS 환경에서만 전송
        refreshCookie.setPath("/"); // 모든 경로에서 쿠키 전송
        refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7일
        response.addCookie(refreshCookie);

        // 3. Access Token → 응답 헤더로 전달 (프론트에서 메모리에 저장)
        response.setHeader("Authorization", "Bearer " + accessToken);

        return ResponseEntity.ok("로그인이 완료되었습니다.");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        String loginId = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        memberService.logout(loginId);

        // Refresh Token 쿠키 만료 처리
        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 즉시 만료
        response.addCookie(refreshCookie);

        return ResponseEntity.ok("로그아웃이 완료되었습니다.");
    }
}