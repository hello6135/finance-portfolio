package com.finance.finportfolio.dto;

import com.finance.finportfolio.domain.Post;

import lombok.Builder;

@Builder
public record PostSaveRequestDto(
        String title,
        String content,
        String author
// String imageUrl // CKEditor방식으로 변경
) {

    // DTO -> Entity 변환 (DB 저장용)
    public Post toEntity() {
        return Post.builder() // Post 엔티티에 @Builder가 있다고 가정 (없으면 일반 생성자 사용)
                .title(title)
                .content(content)
                .author(author)
                // .imageUrl(imageUrl) // CKEditor방식으로 변경
                .build();
    }
}