package com.finance.finportfolio.domain.category.dto;

import com.finance.finportfolio.domain.category.entity.Category;

public record CategoryResponseDto(
        Long id,
        String name,
        int sortOrder) {
    // Entity -> DTO 변환 생성자
    public CategoryResponseDto(Category category) {
        this(
                category.getId(),
                category.getName(),
                category.getSortOrder());
    }
}