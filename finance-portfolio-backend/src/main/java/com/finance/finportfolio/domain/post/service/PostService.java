package com.finance.finportfolio.domain.post.service;

import java.util.List;
import java.util.Objects;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.category.entity.Category;
import com.finance.finportfolio.domain.category.repository.CategoryRepository;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.domain.post.dto.PostResponseDto;
import com.finance.finportfolio.domain.post.dto.PostSaveRequestDto;
import com.finance.finportfolio.domain.post.dto.PostUpdateRequestDto;
import com.finance.finportfolio.domain.post.entity.Post;
import com.finance.finportfolio.domain.post.repository.PostRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// PostService - 게시판 로직 처리
@Slf4j
@Service("postService")
@Transactional
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final FileService fileService;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    // 실제 <img> 태그가 1개 이상 존재하는지 "객체" 단위로 확인
    private boolean checkImage(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty())
            return false;

        Document doc = Jsoup.parseBodyFragment(htmlContent);
        return !doc.select("img").isEmpty();
    }

    // 작성자 체크
    @Transactional(readOnly = true)
    public boolean isPostOwner(Long id, String memberId) {
        if (id == null) {
            return false;
        }
        return postRepository.findById(id)
                .map(post -> {
                    if (post.getAuthor() == null) {
                        return false;
                    }
                    return post.getAuthor().getLoginId().equals(memberId);
                })
                .orElse(false);
    }

    // 게시글 페이지 조회(페이징), return: 게시글 목록
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPostList(int page, int size, Long categoryId) {
        // 최신순 정렬을 포함한 Pageable 객체 생성 (0부터 시작)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        Page<Post> postPage;

        if (categoryId != null && categoryId > 0) {
            postPage = postRepository.findByCategoryId(categoryId, pageable);
        } else {
            postPage = postRepository.findAll(pageable);
        }

        // Entity를 DTO로 변환하여 반환
        return postPage.map(PostResponseDto::from);
    }

    // ID로 게시글 조회, return: 게시글
    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id, String currentLoginId) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        String processedContent = fileService.convertToCdnUrls(post.getContent());

        return PostResponseDto.ofForJsoup(post, processedContent, currentLoginId);
    }

    // 게시글 저장, return: 저장된 게시글 ID
    @Transactional
    public Long savePost(PostSaveRequestDto requestDto, String loginId) {

        Member member = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 카테고리 존재 여부 확인 및 조회
        Category category = categoryRepository.findById(requestDto.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. ID: " + requestDto.categoryId()));

        // Cdn삭제(키 추출)
        String cleanedContent = fileService.removeCdnUrls(requestDto.content());
        boolean hasImage = checkImage(cleanedContent);

        Post post = requestDto.toEntity(category, member, cleanedContent, hasImage);

        return postRepository.save(post).getId();
    }

    // 게시글 수정, 더티 체킹
    @Transactional
    public void updatePost(Long id, PostUpdateRequestDto requestDto) {
        // 1차 캐시에 jpa가 엔티티, 스냅샷 저장
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        // Cdn삭제(키 추출)
        String cleanedContent = fileService.removeCdnUrls(requestDto.content());
        boolean hasImage = checkImage(cleanedContent);

        Category category = categoryRepository.findById(requestDto.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다. ID: " + requestDto.categoryId()));

        // 여기서 엔티티 값 바꿔서 스냅샷이랑 차이나게 -> 더티체킹으로 DB update
        post.update(category, requestDto.title(), cleanedContent, hasImage);
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
