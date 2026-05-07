package com.finance.finportfolio.domain.category.dto;

import com.finance.finportfolio.domain.category.entity.Category;

public record CategoryResponseDto(
        Long id,
        String name,
        int sortOrder) {
    // Entity -> DTO 변환 생성자
    public static CategoryResponseDto from(Category category) {
        return new CategoryResponseDto(
                category.getId(),
                category.getName(),
                category.getSortOrder());
    }
}