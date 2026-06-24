package com.finance.finportfolio.domain.member.dto;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class MemberLoginRequestTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("올바른 로그인 데이터는 검증을 통과")
    void login_Success() {
        MemberLoginRequestDto request = new MemberLoginRequestDto("testId", "password123");

        Set<ConstraintViolation<MemberLoginRequestDto>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("아이디가 공백이면 검증에 실패해야 한다.")
    void login_Fail_BlankId() {
        MemberLoginRequestDto request = new MemberLoginRequestDto("", "1234");

        Set<ConstraintViolation<MemberLoginRequestDto>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("아이디를 입력해주세요.");
    }
}
