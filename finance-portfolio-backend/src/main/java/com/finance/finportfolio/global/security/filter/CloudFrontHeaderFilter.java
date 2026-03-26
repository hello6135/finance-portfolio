package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

// Component에 등록하면 호출과 무관하게 헤더 검증 로직이 작동하는 문제가 있음!: @Value 사용 불가
public class CloudFrontHeaderFilter extends OncePerRequestFilter {

    private final String cfHeaderName;
    private final String cfHeaderValue;

    // 생성자를 통해 값을 직접 주입받음
    public CloudFrontHeaderFilter(String name, String value) {
        this.cfHeaderName = name;
        this.cfHeaderValue = value;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. OPTIONS 요청(CORS) 무조건 통과
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                filterChain.doFilter(request, response);
                return;
            }

            // 2. Health Check 예외 처리
            if ("/api/health".equals(request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }

            // 3. 헤더 검증
            String headerValue = request.getHeader(cfHeaderName);

            if (headerValue == null || !headerValue.equals(cfHeaderValue)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(
                        "{\"status\": 403, \"message\": \"Direct access is not allowed. Please access through CloudFront.\"}");
                return;
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            throw new RuntimeException("CloudFront Header Filter 초기화 실패: " + e.getMessage());
        }

    }
}