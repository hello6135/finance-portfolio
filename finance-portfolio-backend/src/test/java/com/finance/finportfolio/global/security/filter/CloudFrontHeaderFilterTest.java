package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.global.error.exception.CloudFrontConfigurationException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CloudFrontHeaderFilterTest {

    private CloudFrontHeaderFilter filter;
    private static final String HEADER_NAME = "X-Custom-CF-Header";
    private static final String HEADER_VALUE = "secret-value-123";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // 테스트용 필터 인스턴스 생성
        filter = new CloudFrontHeaderFilter(HEADER_NAME, HEADER_VALUE, objectMapper);
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
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("CloudFront를 통하지 않은 직접 접근은 허가되지 않습니다.");
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
    @DisplayName("필터 생성 시 필수 설정값이 누락되면 CloudFrontConfigurationException이 발생한다")
    void shouldThrowExceptionWhenConfigurationIsMissing() {
        // when & then
        assertThatThrownBy(() -> new CloudFrontHeaderFilter("", HEADER_VALUE, objectMapper))
                .isInstanceOf(CloudFrontConfigurationException.class)
                .hasMessageContaining("CloudFront 필터 초기화 실패: 필수 헤더 설정값이 누락되었습니다.");

        assertThatThrownBy(() -> new CloudFrontHeaderFilter(HEADER_NAME, null, objectMapper))
                .isInstanceOf(CloudFrontConfigurationException.class)
                .hasMessageContaining("CloudFront 필터 초기화 실패: 필수 헤더 설정값이 누락되었습니다.");
    }

    @Test
    @DisplayName("필터 체인 동작 중 예기치 않은 예외가 발생하면 500 Internal Server Error를 직접 반환한다")
    void shouldReturn500WhenUnexpectedExceptionOccurs() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HEADER_NAME, HEADER_VALUE); // 일단 헤더 검증은 통과하도록 유도
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 다음 필터로 넘어갈 때 강제로 예외를 발생시키도록 모킹
        FilterChain filterChain = mock(FilterChain.class);
        doThrow(new RuntimeException("DB Connection Failed")).when(filterChain).doFilter(request, response);

        // when
        filter.doFilter(request, response, filterChain);

        // then
        // 예외가 밖으로 던져지지 않고 내부 catch 블록에서 처리되어 500 코드가 나가야 함
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("서버 내부 오류가 발생했습니다.");
    }
}