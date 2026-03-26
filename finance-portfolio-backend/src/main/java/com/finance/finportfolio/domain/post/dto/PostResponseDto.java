package com.finance.finportfolio.domain.post.dto;

import java.time.LocalDateTime;

import com.finance.finportfolio.domain.post.entity.Post;

// PostResponseDto - Post 엔티티 Get용 DTO
public record PostResponseDto(
        Long id,
        String author,
        String title,
        String content,
        boolean hasImage,
        LocalDateTime createdAt) {
    // Entity -> DTO 변환을 위한 생성자
    public PostResponseDto(Post post) {
        this(post, null);
    }

    public PostResponseDto(Post post, String processedContent) {
        this(
                post.getId(),
                post.getAuthor(),
                post.getTitle(),
                processedContent,
                post.isHasImage(),
                post.getCreatedAt());
    }
}