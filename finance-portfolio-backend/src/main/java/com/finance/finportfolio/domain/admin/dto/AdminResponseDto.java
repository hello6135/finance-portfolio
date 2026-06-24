package com.finance.finportfolio.domain.admin.dto;

public record AdminResponseDto(
        long totalPostCount,
        long totalImageCount,
        long totalMemberCount) {
    public static AdminResponseDto from(long totalPostCount, long totalImageCount, long totalMemberCount) {
        return new AdminResponseDto(
                totalPostCount, totalImageCount, totalMemberCount);
    }
}
