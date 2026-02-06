package com.finance.finportfolio.controller;

// Java Util
import java.util.List;

// Spring Web Annotation
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.finance.finportfolio.dto.PostResponseDto;
import com.finance.finportfolio.dto.PostSaveRequestDto;
import com.finance.finportfolio.service.PostService;

// Lombok
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PostController - 게시판 관련 HTTP 요청을 처리하는 Controller 레이어 (MVC)
 *
 * - Controller: 요청/응답, Model/View 연결
 * - Service: 비즈니스 로직
 * - Repository: DB 접근
 */
@Slf4j
@Controller
@RequiredArgsConstructor // 생성자 주입을 위해 추가
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public String list(Model model) {

        // DTO방식으로 변경
        // model.addAttribute("posts", postService.getAllPosts());
        // log.info("sdfsdf");
        // return "posts";

        // 서비스는 Post 엔티티가 아닌 PostResponseDto 리스트를 반환합니다.
        List<PostResponseDto> posts = postService.getAllPosts();
        model.addAttribute("posts", posts);

        log.info("게시글 목록 조회 완료 - 개수: {}", posts.size());
        return "posts";
    }

    // DTO방식으로 변경
    // @PostMapping("/save")
    // public String save(@ModelAttribute Post post) {
    // postService.savePost(post);
    // return "redirect:/posts";
    // }
    // @PostMapping("/save")
    // public String save(@ModelAttribute PostSaveRequestDto requestDto) {
    // // 엔티티 대신 RequestDto를 파라미터로 받습니다.
    // postService.savePost(requestDto);
    // return "redirect:/posts";
    // } > 이미지 포함 방식으로 변경

    // @PostMapping("/save")
    // public String save(@ModelAttribute PostSaveRequestDto requestDto,
    // @RequestParam(value = "image", required = false) MultipartFile image) {
    // > CKEditor 방식으로 변경

    @PostMapping("/save")
    public String save(@ModelAttribute PostSaveRequestDto requestDto) {

        log.info("게시글 저장 시도 - 제목: {}", requestDto.title());

        // 3. 서비스 호출 시 이미지 전달 (이미 수정한 PostService의 규격에 맞춤)
        postService.savePost(requestDto);

        return "redirect:/posts";
    }

    //
    // @DeleteMapping("/{id}") // 이제 "/posts/delete/{id}" 같은 경로 대신 REST 형식을 씁니다.
    // public String delete(@PathVariable Long id) {
    // postService.deletePost(id);
    // return "redirect:/posts";
    // }
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        postService.deletePost(id);
        log.info("게시글 삭제 완료 - ID: {}", id);
        return "redirect:/posts";
    }

}
