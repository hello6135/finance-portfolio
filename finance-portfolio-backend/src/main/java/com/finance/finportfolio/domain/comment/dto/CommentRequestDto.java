package com.finance.finportfolio.domain.comment.dto;

public record CommentRequestDto(
        Long postId,
        Long parentId,
        String content
) {
}
