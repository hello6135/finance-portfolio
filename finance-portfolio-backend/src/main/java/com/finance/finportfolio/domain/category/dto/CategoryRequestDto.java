package com.finance.finportfolio.domain.category.dto;

import com.finance.finportfolio.domain.category.entity.Category;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record CategoryRequestDto(
        @NotBlank(message = "카테고리 이름은 필수입니다.") String name,

        int sortOrder) {
    // DTO -> Entity
    public Category toEntity() {
        return Category.builder()
                .name(name)
                .sortOrder(sortOrder)
                .build();
    }
}
