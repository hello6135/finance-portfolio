package com.finance.finportfolio.domain.post.dto;

import com.finance.finportfolio.domain.category.entity.Category;
import com.finance.finportfolio.domain.post.entity.Post;

import lombok.Builder;

// PostSaveRequestDto - Post 엔티티 Set 용 DTO
@Builder
public record PostSaveRequestDto(
                Long categoryId,
                String author,
                String title,
                String content,
                Boolean hasImage) {

        public PostSaveRequestDto {
                java.util.Objects.requireNonNull(categoryId, "categoryId는 필수입니다.");
                if (hasImage == null) {
                        hasImage = false;
                }
        }

        // DTO -> Entity 변환 (DB 저장용 builder)
        public Post toEntity(Category category, String author, String cleanedContent, Boolean hasImage) {
                return Post.builder()
                                .category(category)
                                .author(author)
                                .title(title)
                                .content(cleanedContent)
                                .hasImage(hasImage)
                                .build();
        }
}