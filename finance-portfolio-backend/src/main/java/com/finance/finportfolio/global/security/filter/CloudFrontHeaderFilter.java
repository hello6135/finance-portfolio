package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.finance.finportfolio.global.error.exception.CloudFrontConfigurationException;

import java.io.IOException;

@Component // 빈 등록은 하되
@ConditionalOnProperty( // dev/prod 프로파일에서만 활성화
        name = "cloudfront.enabled", havingValue = "true")
public class CloudFrontHeaderFilter extends OncePerRequestFilter {

    private final String cfHeaderName;
    private final String cfHeaderValue;

    public CloudFrontHeaderFilter(
            @Value("${cloudfront.custom.header.name}") String cfHeaderName,
            @Value("${cloudfront.custom.header.value}") String cfHeaderValue) {
        this.cfHeaderName = cfHeaderName;
        this.cfHeaderValue = cfHeaderValue;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

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
            throw new CloudFrontConfigurationException("CloudFront Header Filter 초기화 실패: " + e.getMessage());
        }

    }
}