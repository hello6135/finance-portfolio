package com.finance.finportfolio.global.error;

import com.finance.finportfolio.global.error.exception.DuplicateResourceException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authentication.BadCredentialsException;

@RestController
public class TestExceptionController {

    @GetMapping("/test/illegal-argument")
    public void throwIllegalArgument() {
        throw new IllegalArgumentException("특정 값이 잘못되었습니다.");
    }

    @GetMapping("/test/illegal-state-default")
    public void throwIllegalStateDefault() {
        throw new IllegalStateException(""); // 메시지 비움 -> 🛠 기본 메시지 작동 확인
    }

    @GetMapping("/test/bad-credentials")
    public void throwBadCredentials() {
        throw new BadCredentialsException("비밀번호 불일치");
    }

    @GetMapping("/test/duplicate")
    public void throwDuplicate() {
        throw new DuplicateResourceException("이미 가입된 이메일입니다.");
    }

    @GetMapping("/test/runtime")
    public void throwRuntime() {
        throw new RuntimeException("DB 연결 오류");
    }
}