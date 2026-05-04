package com.finance.finportfolio.global.security.filter;

import java.time.Duration;
import java.util.List;

public enum ApiRateLimitGroup {
    // 로그인, 회원가입
    CRITICAL("보안/비용 위험", 1, 5, Duration.ofMinutes(1), List.of(
            "/api/member/login",
            "/api/member/join")),
    // 이미지 S3 업로드, 미참조 이미지 삭제
    HIGH("고부하 리소스", 2, 15, Duration.ofMinutes(1), List.of(
            "/api/image/upload",
            "/api/posts/cleanup")),
    // 게시글 조회, 금융 계산기
    MEDIUM("DB/UX 보호", 3, 30, Duration.ofMinutes(1), List.of(
            "/api/posts",
            "/api/posts/list",
            "/api/finance/fair")),
    LOW("일반 조회", 4, 150, Duration.ofMinutes(1), List.of(
            "/**"// 나머지 모든 경로
    ));

    private final String description;
    private final int priority;
    private final int limit;
    private final Duration duration;
    private final List<String> patterns;

    ApiRateLimitGroup(String description, int priority, int limit, Duration duration, List<String> patterns) {
        this.description = description;
        this.priority = priority;
        this.limit = limit;
        this.duration = duration;
        this.patterns = patterns;
    }

    public String getDescription() {
        return description;
    }

    public int getLimit() {
        return limit;
    }

    public Duration getDuration() {
        return duration;
    }

    public List<String> getPatterns() {
        return patterns;
    }
}
