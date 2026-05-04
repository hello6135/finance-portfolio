package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    // ──────────────────────────────────────────────
    // 1. CRITICAL 그룹 - 로그인 한도 초과 (limit=5)
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("CRITICAL 그룹: 허용량(5회) 초과 시 429와 올바른 메시지를 반환한다")
    void critical_exceedsRateLimitReturns429() throws ServletException, IOException {
        String clientIp = "127.0.0.1";

        // 5회까지는 성공
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = createRequest("/api/member/login", "POST", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        // 6번째 요청: 한도 초과
        MockHttpServletRequest exceedRequest = createRequest("/api/member/login", "POST", clientIp);
        MockHttpServletResponse exceedResponse = new MockHttpServletResponse();

        filter.doFilter(exceedRequest, exceedResponse, filterChain);

        assertThat(exceedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(exceedResponse.getContentType()).isEqualTo("application/json;charset=UTF-8");
        // 변경된 에러 메시지 검증
        assertThat(exceedResponse.getContentAsString())
                .contains("요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.");
        // 응답 JSON에 그룹 설명 포함 여부
        assertThat(exceedResponse.getContentAsString())
                .contains(ApiRateLimitGroup.CRITICAL.getDescription());

        // 6번째 요청에서는 filterChain이 호출되지 않아야 함
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    // ──────────────────────────────────────────────
    // 2. HIGH 그룹 - 이미지 업로드 한도 초과 (limit=15)
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("HIGH 그룹: 허용량(15회) 초과 시 429를 반환한다")
    void high_exceedsRateLimitReturns429() throws ServletException, IOException {
        String clientIp = "10.0.0.1";

        // 15회까지는 성공
        for (int i = 0; i < 15; i++) {
            MockHttpServletRequest request = createRequest("/api/image/upload", "POST", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        // 16번째 요청: 한도 초과
        MockHttpServletRequest exceedRequest = createRequest("/api/image/upload", "POST", clientIp);
        MockHttpServletResponse exceedResponse = new MockHttpServletResponse();

        filter.doFilter(exceedRequest, exceedResponse, filterChain);

        assertThat(exceedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(exceedResponse.getContentAsString())
                .contains(ApiRateLimitGroup.HIGH.getDescription());
        verify(filterChain, times(15)).doFilter(any(), any());
    }

    // ──────────────────────────────────────────────
    // 3. 제외 경로 - filterChain을 그대로 통과해야 함
    // ──────────────────────────────────────────────
    @ParameterizedTest
    @CsvSource({
            "/api/member/logout, POST",
            "/api/member/reissue, POST",
            "/error, GET"
    })
    @DisplayName("제외 경로는 Rate Limit 없이 통과한다")
    void excludedPaths_passThrough(String path, String method) throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = createRequest(path, method, "127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    // ──────────────────────────────────────────────
    // 4. /api/posts 메서드 분기 - POST만 MEDIUM 그룹
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("/api/posts POST 요청은 MEDIUM 그룹(limit=30)으로 처리된다")
    void posts_postMethod_appliesMediumGroup() throws ServletException, IOException {
        String clientIp = "192.168.0.1";

        // MEDIUM limit(30)까지 성공
        for (int i = 0; i < 30; i++) {
            MockHttpServletRequest request = createRequest("/api/posts", "POST", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        // 31번째 요청: MEDIUM 한도 초과
        MockHttpServletRequest exceedRequest = createRequest("/api/posts", "POST", clientIp);
        MockHttpServletResponse exceedResponse = new MockHttpServletResponse();

        filter.doFilter(exceedRequest, exceedResponse, filterChain);

        assertThat(exceedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(exceedResponse.getContentAsString())
                .contains(ApiRateLimitGroup.MEDIUM.getDescription());
    }

    @Test
    @DisplayName("/api/posts GET 요청은 MEDIUM이 아닌 LOW 그룹(limit=150)으로 처리된다")
    void posts_getMethod_appliesLowGroup() throws ServletException, IOException {
        String clientIp = "192.168.0.2";

        // LOW limit(150)까지 성공해야 함 (MEDIUM limit=30을 넘어도 통과)
        for (int i = 0; i < 31; i++) {
            MockHttpServletRequest request = createRequest("/api/posts", "GET", clientIp);
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            // MEDIUM(30)을 초과해도 LOW 그룹이므로 429가 아님
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
    }

    // ──────────────────────────────────────────────
    // 5. IP 독립성 - 다른 IP는 별도 버킷을 사용
    // ──────────────────────────────────────────────
    @Test
    @DisplayName("IP가 다르면 버킷이 독립적으로 관리된다")
    void differentIps_haveSeparateBuckets() throws ServletException, IOException {
        String ipA = "1.1.1.1";
        String ipB = "2.2.2.2";

        // IP-A: CRITICAL 한도(5회) 소진
        for (int i = 0; i < 5; i++) {
            filter.doFilter(createRequest("/api/member/login", "POST", ipA),
                    new MockHttpServletResponse(), filterChain);
        }

        // IP-A 6번째: 429 반환
        MockHttpServletResponse responseA = new MockHttpServletResponse();
        filter.doFilter(createRequest("/api/member/login", "POST", ipA), responseA, filterChain);
        assertThat(responseA.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        // IP-B: 아직 버킷이 가득 차지 않았으므로 통과
        MockHttpServletResponse responseB = new MockHttpServletResponse();
        filter.doFilter(createRequest("/api/member/login", "POST", ipB), responseB, filterChain);
        assertThat(responseB.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────
    private MockHttpServletRequest createRequest(String uri, String method, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        request.setMethod(method);
        request.setRemoteAddr(ip);
        return request;
    }
}