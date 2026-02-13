package com.finance.finportfolio.domain.post.controller;

import java.util.List;

// 스프링 어노테이션
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.finance.finportfolio.domain.post.dto.PostResponseDto;
import com.finance.finportfolio.domain.post.dto.PostSaveRequestDto;
import com.finance.finportfolio.domain.post.dto.PostUpdateRequestDto;
import com.finance.finportfolio.domain.post.service.PostService;

// 롬복
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// PostController - 게시판 관련 HTTP 요청을 처리
@Slf4j
@Controller
@RequiredArgsConstructor // final 불변성, 컴파일 체크, 테스트 용이
@RequestMapping("/posts")
public class PostController {

    // 의존성 주입
    private final PostService postService;

    @GetMapping
    public String postList(Model model) {

        // 서비스는 Post 엔티티가 아닌 PostResponseDto 리스트를 반환
        List<PostResponseDto> posts = postService.getAllPosts();
        model.addAttribute("posts", posts);

        log.info("게시글 목록 조회 완료 - 개수: {}", posts.size());
        return "posts";
    }

    // post-detail.html return
    @GetMapping("/detail/{id}")
    public String toDetailHtml(@PathVariable Long id, Model model) {
        PostResponseDto dto = postService.getPostById(id);

        log.info("게시글 상세 페이지로 - 제목: {}", dto.getTitle());

        model.addAttribute("post", dto);
        return "post-detail";
    }

    // post-editor.html return (with no ID: new Post)
    @GetMapping("/editor")
    public String toEditorHtml(Model model) {
        model.addAttribute("post", null);

        log.info("게시글 작성 페이지로(with no ID)");

        return "post-editor";
    }

    @PostMapping("/editor/create")
    public String createPost(@ModelAttribute PostSaveRequestDto requestDto) {

        log.info("게시글 저장 시도 - 제목: {}", requestDto.title());

        // 3. 서비스 호출 시 이미지 전달 (이미 수정한 PostService의 규격에 맞춤)
        postService.savePost(requestDto);

        return "redirect:/posts";
    }

    // post-editor html return (with ID: update)
    @GetMapping("/editor/{id}")
    public String toEditorWithID(@PathVariable Long id, Model model) {
        PostResponseDto dto = postService.getPostById(id); // 기존에 만든 상세조회 활용

        log.info("게시글 수정 페이지로 - 제목: {}", dto.getTitle());

        model.addAttribute("post", dto);
        return "post-editor";
    }

    @PostMapping("/editor/{id}")
    public String updatePost(@PathVariable Long id, @ModelAttribute PostUpdateRequestDto requestDto) {

        log.info("게시글 수정 시도 - 제목: {}", requestDto.title());

        postService.updatePost(id, requestDto);
        return "redirect:/posts"; // 수정 후 목록으로 리다이렉트
    }

    @DeleteMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        log.info("게시글 삭제 완료 - ID: {}", id);
        return "redirect:/posts";
    }

}
