package com.finance.finportfolio.domain.comment.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.comment.dto.CommentRequestDto;
import com.finance.finportfolio.domain.comment.dto.CommentResponseDto;
import com.finance.finportfolio.domain.comment.service.CommentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<CommentResponseDto>> getCommentsByPost(@PathVariable("postId") Long postId) {
        log.info("게시글 댓글 목록 조회 요청 - Post ID: {}", postId);
        List<CommentResponseDto> comments = commentService.getCommentsByPost(postId);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<Long> createComment(
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal String loginId) {
        log.info("댓글 작성 요청 - Post ID: {}, Parent ID: {}", requestDto.postId(), requestDto.parentId());
        Long commentId = commentService.saveComment(requestDto, loginId);
        return ResponseEntity.status(201).body(commentId);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @commentService.isCommentOwner(#id, authentication.name)")
    public ResponseEntity<Long> updateComment(
            @PathVariable("id") Long id,
            @RequestBody CommentRequestDto requestDto) {
        log.info("댓글 수정 요청 - ID: {}", id);
        commentService.updateComment(id, requestDto);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @commentService.isCommentOwner(#id, authentication.name)")
    public ResponseEntity<Long> deleteComment(@PathVariable("id") Long id) {
        log.info("댓글 삭제 요청 - ID: {}", id);
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
