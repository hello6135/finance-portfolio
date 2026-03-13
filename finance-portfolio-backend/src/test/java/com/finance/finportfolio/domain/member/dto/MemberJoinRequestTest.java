package com.finance.finportfolio.domain.member.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MemberJoinRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("올바른 회원가입 요청 데이터는 검증을 통과한다")
    void validation_Success() {
        // given
        MemberJoinRequest request = new MemberJoinRequest("testUser", "password123", "테스터");

        // when
        Set<ConstraintViolation<MemberJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("비밀번호가 4자 미만이면 검증에 실패한다")
    void validation_Fail_PasswordSize() {
        // given (비밀번호를 3자로 설정)
        MemberJoinRequest request = new MemberJoinRequest("testUser", "123", "테스터");

        // when
        Set<ConstraintViolation<MemberJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().equals("비밀번호는 최소 4자 이상이어야 합니다.")))
                .isTrue();
    }

    @Test
    @DisplayName("아이디가 공백이면 검증에 실패한다")
    void validation_Fail_NotBlank() {
        // given (아이디를 빈 값으로 설정)
        MemberJoinRequest request = new MemberJoinRequest("", "password123", "테스터");

        // when
        Set<ConstraintViolation<MemberJoinRequest>> violations = validator.validate(request);

        // then
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream()
                .anyMatch(v -> v.getMessage().equals("아이디는 필수입니다.")))
                .isTrue();
    }
}