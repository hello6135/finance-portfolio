package com.finance.finportfolio.domain.post.dto;

import lombok.Builder;

@Builder
public record PostUpdateRequestDto(
        Long categoryId,
        String title,
        String content,
        Boolean hasImage) {
}
