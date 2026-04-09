package com.finance.finportfolio.global.error.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DuplicateResourceExceptionTest {

    @Test
    @DisplayName("메시지를 담은 예외가 정상적으로 생성되고 던져진다")
    void exceptionMessageTest() {
        String message = "이미 존재하는 아이디입니다.";

        assertThatThrownBy(() -> {
            throw new DuplicateResourceException(message);
        }).isInstanceOf(DuplicateResourceException.class)
                .hasMessage(message);
    }

    @Test
    @DisplayName("원인 예외(Cause)를 포함하여 던질 수 있다")
    void exceptionWithCauseTest() {
        Throwable cause = new IllegalArgumentException("원본 에러");
        String message = "래핑된 에러 메시지";

        DuplicateResourceException ex = new DuplicateResourceException(message, cause);

        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}
