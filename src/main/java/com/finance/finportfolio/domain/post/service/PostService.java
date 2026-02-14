package com.finance.finportfolio.domain.post.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.post.domain.Post;
import com.finance.finportfolio.domain.post.domain.PostRepository;
import com.finance.finportfolio.domain.post.dto.PostResponseDto;
import com.finance.finportfolio.domain.post.dto.PostSaveRequestDto;
import com.finance.finportfolio.domain.post.dto.PostUpdateRequestDto;
import com.finance.finportfolio.infrastructure.file.FileService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// PostService - 게시판 로직 처리
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    // jsoup 커스텀 설정 본문용(utext)
    private static final Safelist HTML_SAFE_LIST = Safelist.relaxed()
            .addAttributes("img", "style", "alt", "width", "height") // 이미지 관련 속성 허용
            .addTags("hr", "br"); // 가로줄, 줄바꿈 명시적 허용

    // 의존성 주입
    private final PostRepository postRepository;
    private final FileService fileService;

    /**
     * 모든 게시글 조회
     * 
     * @return 게시글 목록
     */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostResponseDto::new)
                .toList(); // JDK 21 최신 문법
    }

    /**
     * ID로 게시글 조회 (나중에 상세보기 기능에 사용)
     * 
     * @param id 조회할 게시글의 ID
     * @return 게시글 (없으면 null)
     */
    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        return new PostResponseDto(post);
    }

    // Jsoup 소독 메서드
    private String cleanText(String text) {
        return (text == null) ? "" : Jsoup.clean(text, Safelist.none());
    }

    private String cleanHtml(String text) {
        return (text == null) ? "" : Jsoup.clean(text, HTML_SAFE_LIST);
    }

    private boolean checkImage(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty())
            return false;

        Document doc = Jsoup.parseBodyFragment(htmlContent);
        // 실제 <img> 태그가 1개 이상 존재하는지 "객체" 단위로 확인
        return !doc.select("img").isEmpty();
    }

    /**
     * 게시글 저장
     * 
     * @param post 저장할 게시글 객체
     * @return 저장된 게시글
     */
    @Transactional
    public Long savePost(PostSaveRequestDto requestDto) {
        String cleanedContent = cleanHtml(requestDto.content());
        boolean hasImage = checkImage(cleanedContent);

        Post post = Post.builder()
                .author(cleanText(requestDto.author()))
                .title(cleanText(requestDto.title()))
                .content(cleanedContent)
                .hasImage(hasImage)
                .build();

        // Repository를 통해 DB 저장 후 ID 반환
        return postRepository.save(post).getId();
    }

    @Transactional
    public void updatePost(Long id, PostUpdateRequestDto requestDto) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        String cleanedContent = cleanHtml(requestDto.content());
        boolean hasImage = checkImage(cleanedContent);

        post.update(cleanText(requestDto.title()), cleanedContent, hasImage);
    }

    /**
     * 게시글 삭제
     * 
     * @param id 삭제할 게시글의 ID
     */
    @Transactional
    public void deletePost(Long id) {
        // 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제하려는 게시글이 존재하지 않습니다. id=" + id));

        log.info("파일 삭제 요청 id: {}", id);
        // 이미지 삭제: 해당 게시글 content에서 이미지 파일명들 추출 후 물리적 삭제
        fileService.deleteFile(post.getContent());

        // 게시글 삭제
        postRepository.delete(post);
    }

    // HTML 문자열에서 파일명만 추출하는 메서드

    // 비참조 파일 삭제 요청(Local환경은 버튼, AWS 환경에서는 정시 반복으로 구현 예정)
    @Transactional(readOnly = true)
    public List<String> cleanUpOrphanFiles() { // 리턴 타입을 List로 변경
        List<String> allPostContents = postRepository.findAll().stream()
                .map(Post::getContent)
                .filter(Objects::nonNull)
                .toList();

        // fileService가 삭제한 목록을 받아서 그대로 컨트롤러에 전달
        return fileService.cleanUpOrphanFiles(allPostContents);
    }

}
