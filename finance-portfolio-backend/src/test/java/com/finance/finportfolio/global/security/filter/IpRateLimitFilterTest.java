package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IpRateLimitFilterTest {

    private IpRateLimitFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new IpRateLimitFilter();
        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("허용량(5회) 초과 요청 시 429 에러와 메시지를 반환한다")
    void exceedsRateLimitReturns429() throws ServletException, IOException {
        // given: 동일한 IP로 요청 설정
        String clientIp = "127.0.0.1";

        // 5회까지는 성공해야 함
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = createLoginRequest(clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        // when: 6번째 요청
        MockHttpServletRequest exceedRequest = createLoginRequest(clientIp);
        MockHttpServletResponse exceedResponse = new MockHttpServletResponse();

        filter.doFilter(exceedRequest, exceedResponse, filterChain);

        // then: 429 Too Many Requests 확인
        assertThat(exceedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(exceedResponse.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(exceedResponse.getContentAsString()).contains("요청이 너무 많습니다.");

        // 6번째 요청은 filterChain.doFilter가 호출되지 않았어야 함
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    @Test
    @DisplayName("로그인 경로가 아닌 경우 필터를 통과한다")
    void skipFilterForNonLoginPath() throws ServletException, IOException {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/member/profiles");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        verify(filterChain, times(1)).doFilter(request, response);
    }

    private MockHttpServletRequest createLoginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/member/login");
        request.setRemoteAddr(ip);
        return request;
    }
}