package com.finance.finportfolio.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.global.error.exception.CloudFrontConfigurationException;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component // 빈 등록은 하되
@ConditionalOnProperty( // dev/prod 프로파일에서만 활성화
        name = "cloudfront.enabled", havingValue = "true")
public class CloudFrontHeaderFilter extends OncePerRequestFilter {

    private final String cfHeaderName;
    private final String cfHeaderValue;
    private final ObjectMapper objectMapper;

    public CloudFrontHeaderFilter(
            @Value("${cloudfront.custom.header.name}") String cfHeaderName,
            @Value("${cloudfront.custom.header.value}") String cfHeaderValue,
            ObjectMapper objectMapper) {

        // 서버 실행시점 필수 값 체크
        if (!StringUtils.hasText(cfHeaderName) || !StringUtils.hasText(cfHeaderValue)) {
            throw new CloudFrontConfigurationException("CloudFront 필터 초기화 실패: 필수 헤더 설정값이 누락되었습니다.");
        }

        this.cfHeaderName = cfHeaderName;
        this.cfHeaderValue = cfHeaderValue;
        this.objectMapper = objectMapper;
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
                sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                        "CloudFront를 통하지 않은 직접 접근은 허가되지 않습니다.");
                return;
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("CloudFront 헤더 검증 실패: {}", e.getMessage());

            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> errorBody = Map.of(
                "status", status,
                "message", message);
        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}