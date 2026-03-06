package com.finance.finportfolio.domain.post.dto;

import com.finance.finportfolio.domain.post.domain.Post;

import lombok.Builder;

// PostSaveRequestDto - Post 엔티티 Set 용 DTO
@Builder
public record PostSaveRequestDto(
                String author,
                String title,
                String content,
                Boolean hasImage) {

        public PostSaveRequestDto {
                if (hasImage == null) {
                        hasImage = false;
                }
        }

        // DTO -> Entity 변환 (DB 저장용)
        public Post toEntity() {
                return Post.builder()
                                .author(author)
                                .title(title)
                                .content(content)
                                .hasImage(hasImage)
                                .build();
        }
}