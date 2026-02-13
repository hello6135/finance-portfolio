package com.finance.finportfolio.domain.post.dto;

import lombok.Builder;

@Builder
public record PostUpdateRequestDto(
                String title,
                String content,
                Boolean hasImage) {
}
