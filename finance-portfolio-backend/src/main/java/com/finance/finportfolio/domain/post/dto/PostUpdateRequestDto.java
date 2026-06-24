package com.finance.finportfolio.domain.post.dto;

import lombok.Builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Builder
public record PostUpdateRequestDto(

                @NotNull(message = "카테고리는 필수 선택 사항입니다.") Long categoryId,
                @NotBlank(message = "제목을 입력해주세요.") @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.") String title,
                @NotBlank(message = "본문을 입력해주세요.") String content,
                Boolean hasImage) {
}
