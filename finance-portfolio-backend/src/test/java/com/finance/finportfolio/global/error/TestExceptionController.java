package com.finance.finportfolio.global.error;

import com.finance.finportfolio.global.error.exception.CloudFrontConfigurationException;
import com.finance.finportfolio.global.error.exception.DuplicateResourceException;
import com.finance.finportfolio.global.error.exception.RefreshTokenNotFoundException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@RestController
public class TestExceptionController {

    @GetMapping("/test/illegal-argument")
    public void throwIllegalArgument() {
        throw new IllegalArgumentException("특정 값이 잘못되었습니다."); // 메시지 넣음 -> 명시 메시지 작동 확인
    }

    @GetMapping("/test/illegal-state-default")
    public void throwIllegalStateDefault() {
        throw new IllegalStateException(""); // 메시지 비움 -> 🛠 기본 메시지 작동 확인
    }

    @GetMapping("/test/user-not-found")
    public void throUsernameNotFound() {
        throw new UsernameNotFoundException("");
    }

    @GetMapping("/test/bad-credentials")
    public void throwBadCredentials() {
        throw new BadCredentialsException("");
    }

    @GetMapping("/test/refresh-token-not-found")
    public void throwRefreshTokenNotFound() {
        throw new RefreshTokenNotFoundException("");
    }

    @GetMapping("/test/duplicate")
    public void throwDuplicate() {
        throw new DuplicateResourceException("이미 가입된 이메일입니다.");
    }

    @GetMapping("/test/cloudfront-config-error")
    public void throwCloudFrontConfiguration() {
        throw new CloudFrontConfigurationException("");
    }

    @GetMapping("/test/runtime")
    public void throwRuntime() {
        throw new RuntimeException("DB 연결 오류");
    }
}