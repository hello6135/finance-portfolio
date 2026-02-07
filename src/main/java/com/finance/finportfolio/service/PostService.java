package com.finance.finportfolio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.Post;
import com.finance.finportfolio.domain.PostRepository;
import com.finance.finportfolio.dto.PostResponseDto;
import com.finance.finportfolio.dto.PostSaveRequestDto;
import com.finance.finportfolio.infrastructure.FileHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// PostService - 게시판 로직 처리
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class PostService {

    // 의존성 주입
    private final PostRepository postRepository;
    private final FileHandler fileHandler;

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
     * 게시글 저장
     * 
     * @param post 저장할 게시글 객체
     * @return 저장된 게시글
     */
    @Transactional
    public Long savePost(PostSaveRequestDto requestDto) {
        Post post = Post.builder()
                .title(requestDto.title())
                .content(requestDto.content())
                .author(requestDto.author())
                .build();

        // Repository를 통해 DB 저장 후 ID 반환
        return postRepository.save(post).getId();
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

    /**
     * 게시글 삭제
     * 
     * @param id 삭제할 게시글의 ID
     */
    @Transactional
    public void deletePost(Long id) {
        // DB에서 게시글 조회
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제하려는 게시글이 존재하지 않습니다. id=" + id));

        // 해당 게시글 content에서 이미지 파일명들 추출 후 물리적 삭제
        List<String> fileNames = extractFileNamesFromContent(post.getContent());
        for (String fileName : fileNames) {
            fileHandler.deleteFile(fileName);
        }

        // DB에서도 게시글 삭제 (엔티티 객체로 삭제)
        postRepository.delete(post);
    }

    // HTML 문자열에서 파일명만 추출하는 메서드
    private List<String> extractFileNamesFromContent(String content) {
        List<String> fileNames = new ArrayList<>();
        if (content == null)
            return fileNames;

        // /images/ 뒤에 오는 파일명 패턴을 찾음 (정규표현식)
        Pattern pattern = Pattern.compile("/images/([^\"'>\\s]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            fileNames.add(matcher.group(1)); // 첫 번째 괄호 안의 파일명 부분만 저장
        }
        return fileNames;
    }
}
