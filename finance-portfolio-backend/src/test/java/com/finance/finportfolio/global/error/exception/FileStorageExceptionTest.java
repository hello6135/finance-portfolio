package com.finance.finportfolio.global.error.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileStorageExceptionTest {

    @Test
    @DisplayName("메시지를 담은 예외가 정상적으로 생성되고 던져진다")
    void exceptionMessageTest() {
        String message = "Refresh Token이 없습니다.";

        assertThatThrownBy(() -> {
            throw new FileStorageException(message);
        }).isInstanceOf(FileStorageException.class)
                .hasMessage(message);
    }

    @Test
    @DisplayName("원인 예외(Cause)를 포함하여 던질 수 있다")
    void exceptionWithCauseTest() {
        Throwable cause = new IllegalArgumentException("원본 에러");
        String message = "래핑된 에러 메시지";

        FileStorageException ex = new FileStorageException(message, cause);

        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}