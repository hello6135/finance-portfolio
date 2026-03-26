package com.finance.finportfolio.domain.post.service;

import java.util.List;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.post.dto.PostResponseDto;
import com.finance.finportfolio.domain.post.dto.PostSaveRequestDto;
import com.finance.finportfolio.domain.post.dto.PostUpdateRequestDto;
import com.finance.finportfolio.domain.post.entity.Post;
import com.finance.finportfolio.domain.post.repository.PostRepository;

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

    private final PostRepository postRepository;
    private final FileService fileService;

    // Jsoup 소독 메서드
    private String cleanText(String text) {
        return (text == null || text.isEmpty()) ? "" : Jsoup.clean(text, Safelist.none());
    }

    private String cleanHtml(String text) {
        return (text == null || text.isEmpty()) ? "" : Jsoup.clean(text, HTML_SAFE_LIST);
    }

    private boolean checkImage(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty())
            return false;

        Document doc = Jsoup.parseBodyFragment(htmlContent);
        // 실제 <img> 태그가 1개 이상 존재하는지 "객체" 단위로 확인
        return !doc.select("img").isEmpty();
    }

    // 모든 게시글 조회, return: 게시글 목록
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostResponseDto::new)
                .toList(); // JDK 21 최신 문법
    }

    // ID로 게시글 조회, return: 게시글
    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        String processedContent = fileService.convertToCdnUrls(post.getContent());

        return new PostResponseDto(post, processedContent);
    }

    // 게시글 저장, return: 저장된 게시글 ID
    @Transactional
    public Long savePost(PostSaveRequestDto requestDto) {
        // jsoup 살균과 Cdn삭제(키 추출)
        String cleanedContent = fileService.removeCdnUrls(cleanHtml(requestDto.content()));
        boolean hasImage = checkImage(cleanedContent);

        Post post = Post.builder()
                .author(cleanText(requestDto.author()))
                .title(cleanText(requestDto.title()))
                .content(cleanedContent)
                .hasImage(hasImage)
                .build();

        return postRepository.save(post).getId();
    }

    // 게시글 수정, 더티 체킹
    @Transactional
    public void updatePost(Long id, PostUpdateRequestDto requestDto) {
        // 1차 캐시에 jpa가 엔티티, 스냅샷 저장
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        // jsoup 살균과 Cdn삭제(키 추출)
        String cleanedContent = fileService.removeCdnUrls(cleanHtml(requestDto.content()));
        boolean hasImage = checkImage(cleanedContent);

        // 여기서 엔티티 값 바꿔서 스냅샷이랑 차이나게 -> 더티체킹으로 DB update
        post.update(cleanText(requestDto.title()), cleanedContent, hasImage);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long id) {
        // 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제하려는 게시글이 존재하지 않습니다. id=" + id));

        log.info("게시글 삭제 요청 id: {}", id);
        // S3 이미지 삭제
        fileService.deleteFiles(post.getContent());

        // DB 게시글 삭제
        postRepository.delete(post);
    }

    // 미참조 이미지 파일 삭제 - 버튼 식(차후 정기 실행으로 변경)
    @Transactional(readOnly = true)
    public void cleanUpOrphanFiles() {
        // DB에서 post의 모든 content 넘김
        List<String> allPostContents = postRepository.findAll().stream()
                .map(Post::getContent)
                .filter(Objects::nonNull)
                .toList();

        fileService.cleanUpOrphanFiles(allPostContents);
    }

}
