package com.finance.finportfolio.domain.post.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.post.dto.PostResponseDto;
import com.finance.finportfolio.domain.post.dto.PostSaveRequestDto;
import com.finance.finportfolio.domain.post.dto.PostUpdateRequestDto;
import com.finance.finportfolio.domain.post.service.PostService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    @GetMapping("/list") // posts 게시글 리스트 조회(페이징)
    public ResponseEntity<Page<PostResponseDto>> getPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId) {

        // 서비스 레이어에 page와 size를 전달하여 페이징 결과 수신
        Page<PostResponseDto> postPage = postService.getPostList(page, size, categoryId);
        return ResponseEntity.ok(postPage);
    }

    @GetMapping("/{id}") // post detail, editor 페이지에서 조회
    public ResponseEntity<PostResponseDto> getPostById(@PathVariable("id") Long id) {
        PostResponseDto post = postService.getPostById(id);
        log.info("게시글 상세 조회 - ID: {}, 제목: {}", id, post.title());
        return ResponseEntity.ok(post);
    }

    @PostMapping // 새 post - portfolio_db에 저장
    public ResponseEntity<Long> createPost(@RequestBody PostSaveRequestDto requestDto,
            @AuthenticationPrincipal String loginId) {
        log.info("게시글 저장 시도 - 제목: {}", requestDto.title());
        Long postId = postService.savePost(requestDto, loginId);
        return ResponseEntity.status(201).body(postId);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Long> updatePost(@PathVariable("id") Long id, @RequestBody PostUpdateRequestDto requestDto) {
        log.info("게시글 수정 시도 - ID: {}, 제목: {}", id, requestDto.title());
        postService.updatePost(id, requestDto);
        return ResponseEntity.ok(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Long> deletePost(@PathVariable("id") Long id) {
        log.info("게시글 삭제 시도 - ID: {}", id);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<Void> cleanUpOrphanFiles() {
        log.info("미참조 이미지 정리 시작");
        postService.cleanUpOrphanFiles();
        return ResponseEntity.noContent().build();
    }

}
