package com.finance.finportfolio.dto;

import java.time.LocalDateTime;

import com.finance.finportfolio.domain.Post;

import lombok.Getter;

@Getter
public class PostResponseDto {
    private final Long id;
    private final String title;
    private final String content;
    private final String author;
    // private final String imageUrl; // CKEditor방식으로 변경
    private final LocalDateTime createdAt;

    // Entity -> DTO 변환을 위한 생성자
    public PostResponseDto(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.author = post.getAuthor();
        // this.imageUrl = post.getImageUrl(); // CKEditor방식으로 변경
        this.createdAt = post.getCreatedAt();
    }
}