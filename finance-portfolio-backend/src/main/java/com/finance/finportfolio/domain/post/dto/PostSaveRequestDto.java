package com.finance.finportfolio.domain.post.dto;

import com.finance.finportfolio.domain.category.entity.Category;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.post.entity.Post;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Builder;

// PostSaveRequestDto - Post 엔티티 Set 용 DTO
@Builder
public record PostSaveRequestDto(
                @NotNull(message = "카테고리는 필수 선택 사항입니다.") Long categoryId,
                @NotBlank(message = "작성자는 필수 입력 항목입니다.") String author,
                @NotBlank(message = "제목을 입력해주세요.") @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.") String title,
                @NotBlank(message = "본문을 입력해주세요.") String content,
                Boolean hasImage) {

        public PostSaveRequestDto {
                java.util.Objects.requireNonNull(categoryId, "categoryId는 필수입니다.");
                if (hasImage == null) {
                        hasImage = false;
                }
        }

        // DTO -> Entity 변환 (DB 저장용 builder)
        public Post toEntity(Category category, Member author, String cleanedContent, Boolean hasImage) {
                return Post.builder()
                                .category(category)
                                .author(author)
                                .title(title)
                                .content(cleanedContent)
                                .hasImage(hasImage)
                                .build();
        }
}