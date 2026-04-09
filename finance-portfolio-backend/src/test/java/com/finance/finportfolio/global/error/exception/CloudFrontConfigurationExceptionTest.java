package com.finance.finportfolio.global.error.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CloudFrontConfigurationExceptionTest {

    @Test
    @DisplayName("메시지를 담은 예외가 정상적으로 생성되고 던져진다")
    void exceptionMessageTest() {
        String message = "CloudFront Header Filter 초기화 실패";

        assertThatThrownBy(() -> {
            throw new CloudFrontConfigurationException(message);
        }).isInstanceOf(CloudFrontConfigurationException.class)
                .hasMessage(message);
    }

    @Test
    @DisplayName("원인 예외(Cause)를 포함하여 던질 수 있다")
    void exceptionWithCauseTest() {
        Throwable cause = new IllegalArgumentException("원본 에러");
        String message = "래핑된 에러 메시지";

        CloudFrontConfigurationException ex = new CloudFrontConfigurationException(message, cause);

        assertThat(ex.getMessage()).isEqualTo(message);
        assertThat(ex.getCause()).isEqualTo(cause);
    }
}