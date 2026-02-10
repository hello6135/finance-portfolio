package com.finance.finportfolio.domain.post.dto;

import java.time.LocalDateTime;

import com.finance.finportfolio.domain.post.domain.Post;

import lombok.Getter;

// PostResponseDto - Post 엔티티 Get용 DTO
@Getter
public class PostResponseDto {
    private final Long id;
    private final String author;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;

    // Entity -> DTO 변환을 위한 생성자
    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.author = post.getAuthor();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.createdAt = post.getCreatedAt();
    }
}