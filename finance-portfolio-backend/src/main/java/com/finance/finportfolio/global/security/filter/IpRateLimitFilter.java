package com.finance.finportfolio.global.security.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 요청에 맞는 그룹 식별
        ApiRateLimitGroup group = resolveGroup(uri, method);

        // 예외 경로 처리
        if (isExcludedPath(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 버킷 키 생성(Group + IP)
        String ip = request.getRemoteAddr();
        String key = group.name() + ":" + ip;
        Bucket bucket = buckets.computeIfAbsent(key, k -> createNewBucket(group));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            sendLimitExceededResponse(response, group);
        }
    }

    private ApiRateLimitGroup resolveGroup(String uri, String method) {
        // 우선순위가 높은 그룹부터 매칭 확인
        for (ApiRateLimitGroup group : ApiRateLimitGroup.values()) {
            for (String pattern : group.getPatterns()) {
                if (pathMatcher.match(pattern, uri)) {
                    // /api/posts 의 경우 POST일 때만 MEDIUM 그룹으로 적용하는 등의 세부 제어
                    if (uri.equals("/api/posts") && !method.equalsIgnoreCase("POST")) {
                        continue;
                    }
                    return group;
                }
            }
        }
        return ApiRateLimitGroup.LOW;
    }

    private boolean isExcludedPath(String uri) {
        return uri.startsWith("/api/member/logout") ||
                uri.startsWith("/api/member/reissue") ||
                uri.equals("/error");
    }

    private Bucket createNewBucket(ApiRateLimitGroup group) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(group.getLimit())
                        .refillIntervally(group.getLimit(), group.getDuration())
                        .build())
                .build();
    }

    private void sendLimitExceededResponse(HttpServletResponse response, ApiRateLimitGroup group) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format(
                "{\"status\": 429, \"group\": \"%s\", \"message\": \"요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.\"}",
                group.getDescription());
        response.getWriter().write(json);
    }
}
