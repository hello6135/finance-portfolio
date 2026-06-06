package com.finance.finportfolio.domain.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.finance.finportfolio.domain.comment.entity.Comment;

public record CommentResponseDto(
        Long id,
        String authorNickname,
        String authorLoginId,
        String content,
        LocalDateTime createdAt,
        List<CommentResponseDto> children
) {
    public static CommentResponseDto of(Comment comment, List<CommentResponseDto> children) {
        return new CommentResponseDto(
                comment.getId(),
                comment.getAuthor() != null ? comment.getAuthor().getNickname() : null,
                comment.getAuthor() != null ? comment.getAuthor().getLoginId() : null,
                comment.getContent(),
                comment.getCreatedAt(),
                children
        );
    }
}
