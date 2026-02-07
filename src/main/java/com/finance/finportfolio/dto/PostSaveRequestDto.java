package com.finance.finportfolio.dto;

import com.finance.finportfolio.domain.Post;

import lombok.Builder;

// PostSaveRequestDto - Post 엔티티 Set 용 DTO
@Builder
public record PostSaveRequestDto(
        String author,
        String title,
        String content) {

    // DTO -> Entity 변환 (DB 저장용)
    public Post toEntity() {
        return Post.builder()
                .author(author)
                .title(title)
                .content(content)
                .build();
    }
}