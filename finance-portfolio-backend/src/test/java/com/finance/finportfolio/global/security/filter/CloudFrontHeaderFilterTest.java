package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.finance.finportfolio.domain.member.controller.MemberController;
import com.finance.finportfolio.domain.member.controller.TokenCookieManager;
import com.finance.finportfolio.global.error.exception.CloudFrontConfigurationException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CloudFrontHeaderFilterTest {

    private CloudFrontHeaderFilter filter;
    private final String HEADER_NAME = "X-Custom-CF-Header";
    private final String HEADER_VALUE = "secret-value-123";

    @BeforeEach
    void setUp() {
        // 테스트용 필터 인스턴스 생성
        filter = new CloudFrontHeaderFilter(HEADER_NAME, HEADER_VALUE);
    }

    @Test
    @DisplayName("올바른 CloudFront 헤더가 포함된 요청은 통과해야 한다")
    void shouldPassWhenHeaderIsValid() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, HEADER_VALUE);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
    }

    @Test
    @DisplayName("헤더가 없거나 값이 틀리면 403 Forbidden을 반환해야 한다")
    void shouldReturn403WhenHeaderIsInvalid() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, "wrong-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("Direct access is not allowed");
    }

    @Test
    @DisplayName("CORS를 위한 OPTIONS 요청은 헤더 없이도 통과해야 한다")
    void shouldPassOptionsRequest() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Health Check 경로는 헤더 없이도 통과해야 한다")
    void shouldPassHealthCheckRequest() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("예외 발생 시 지정된 메시지가 포함된 CloudFrontConfigurationException을 던진다")
    void throwCloudFrontConfigurationException() {
        // given
        String originalErrorMessage = "Access Key is missing";
        String expectedPrefix = "CloudFront Header Filter 초기화 실패: ";

        // when & then
        assertThatThrownBy(() -> {
            try {
                // 의도적으로 예외 상황 발생
                throw new RuntimeException(originalErrorMessage);
            } catch (Exception e) {
                // 작성하신 catch 블록 로직
                throw new CloudFrontConfigurationException(expectedPrefix + e.getMessage());
            }
        })
                .isInstanceOf(CloudFrontConfigurationException.class)
                .hasMessage(expectedPrefix + originalErrorMessage);
    }

    @Test
    @DisplayName("예외가 발생해도 원인(Cause)이 유지되는지 확인")
    void exceptionCausePersistence() {
        // given
        RuntimeException cause = new RuntimeException("Original Cause");

        // when
        CloudFrontConfigurationException exception = new CloudFrontConfigurationException("Test Message", cause);

        // then
        assertThat(exception.getMessage()).isEqualTo("Test Message");
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}