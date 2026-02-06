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

/**
 * PostService - 게시판 비즈니스 로직을 담당하는 Service 레이어
 * 
 * 🎓 Service 레이어란?
 * - Controller와 Repository 사이의 중간 계층
 * - 비즈니스 로직(데이터 처리, 검증, 계산 등)을 담당
 * - 왜 필요한가?
 * 1. Controller는 HTTP 요청/응답만 처리 (가볍게 유지)
 * 2. Repository는 DB 접근만 담당 (데이터 저장/조회)
 * 3. Service는 "어떻게 처리할지"의 비즈니스 규칙을 담당
 * 
 * 예시: 게시글 저장 시 제목 길이 제한, 중복 체크, 조회수 증가 등의 로직을 여기서 처리
 */
@Slf4j
@Service // Spring이 이 클래스를 Service Bean으로 등록 (자동으로 의존성 주입 가능)
@Transactional // 메서드들이 트랜잭션 내에서 실행됨 (성공 시 커밋, 실패 시 롤백)
@RequiredArgsConstructor
public class PostService {

    // 2. @Autowired를 지우고 변수 앞에 'final'사용 - 불변성 보장 및 런타임 에러 방지
    private final PostRepository postRepository;
    private final FileHandler fileHandler; // 1. 주입 받기

    /**
     * 모든 게시글 조회
     * 
     * @return 게시글 목록
     */
    // public List<Post> getAllPosts() {
    // // Repository를 통해 DB에서 모든 게시글을 가져옵니다
    // // 나중에 여기에 페이징, 정렬 등의 로직을 추가할 수 있습니다
    // return postRepository.findAll();
    // } DTO방식으로 변경
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostResponseDto::new) // DTO 생성자 활용
                .toList(); // JDK 21 최신 문법
    }

    /**
     * 게시글 저장
     * 
     * @param post 저장할 게시글 객체
     * @return 저장된 게시글
     * 
     *         💡 여기에 비즈니스 로직을 추가할 수 있습니다:
     *         - 제목/내용 길이 검증
     *         - 중복 제목 체크
     *         - 작성 시간 자동 설정
     */
    // public Post savePost(Post post) {
    // // 생성 시간이 설정되지 않았다면 현재 시간으로 설정
    // if (post.getCreatedAt() == null) {
    // post.setCreatedAt(java.time.LocalDateTime.now());
    // }

    // // Repository를 통해 DB에 저장
    // return postRepository.save(post);
    // } > DTO방식으로 변경
    // public Long savePost(PostSaveRequestDto requestDto) {
    // // DTO 내부의 toEntity() 메서드를 호출하여 Post 객체 생성
    // Post post = requestDto.toEntity();

    // // (JPA Auditing으로 생략
    // // if (post.getCreatedAt() == null) {
    // // //post.setCreatedAt(java.time.LocalDateTime.now());
    // // post.setCreatedAt(LocalDateTime.now());
    // // }

    // return postRepository.save(post).getId(); // 저장 후 생성된 ID 반환
    // } > 이미지 파일 포함
    @Transactional
    public Long savePost(PostSaveRequestDto requestDto) {
        // 1. FileHandler를 통해 이미지 저장 후 고유 파일명(UUID) 반환 받기
        // 이미지 파일이 전송되지 않았다면(null) FileHandler 내부 로직에 의해 null이 반환됨
        // String savedImageName = fileHandler.uploadFile(image); // CKEditor방식으로 변경

        // 2. DTO 데이터를 꺼내서 Post 엔티티 생성 (빌더 패턴)
        // DTO의 toEntity()를 쓰는 대신, imageUrl을 합치기 위해 여기서 직접 빌드합니다.
        Post post = Post.builder()
                .title(requestDto.title())
                .content(requestDto.content())
                .author(requestDto.author())
                // .imageUrl(savedImageName) // CKEditor방식으로 변경
                .build();

        // 3. Repository를 통해 DB 저장 후 ID 반환
        return postRepository.save(post).getId();
    }

    /**
     * 게시글 삭제
     * 
     * @param id 삭제할 게시글의 ID
     * 
     *           💡 나중에 추가 가능한 로직:
     *           - 삭제 권한 체크
     *           - 소프트 삭제 (실제 삭제 대신 삭제 표시만)
     */
    // public void deletePost(Long id) {
    // // Repository를 통해 DB에서 삭제
    // postRepository.deleteById(id);
    // } DTO방식으로 변경
    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 게시글이 없습니다. id=" + id));

        return new PostResponseDto(post);
    }

    /**
     * ID로 게시글 조회 (나중에 상세보기 기능에 사용)
     * 
     * @param id 조회할 게시글의 ID
     * @return 게시글 (없으면 null)
     */
    // public Post getPostById(Long id) {
    // return postRepository.findById(id).orElse(null);
    // } DTO방식으로 변경
    // public void deletePost(Long id) {
    // // 존재하는지 먼저 확인하는 로직을 넣으면 더 견고한 코드가 됩니다.
    // if (!postRepository.existsById(id)) {
    // throw new IllegalArgumentException("삭제하려는 게시글이 존재하지 않습니다. id=" + id);
    // }
    // postRepository.deleteById(id);
    // } 이미지 삭제 추가
    @Transactional
    public void deletePost(Long id) {
        // 1. DB에서 게시글 조회 (파일 이름을 알아내기 위해 필요)
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("삭제하려는 게시글이 존재하지 않습니다. id=" + id));

        // 2. 물리적 이미지 파일 삭제 로직
        // fileHandler.deleteFile(post.getImageUrl()); CKEditor 방식으로 변경
        // 1. content(HTML)에서 이미지 파일명들 추출
        List<String> fileNames = extractFileNamesFromContent(post.getContent());

        // 2. 추출된 파일명들 하나씩 물리적 삭제
        for (String fileName : fileNames) {
            fileHandler.deleteFile(fileName);
        }

        // 3. DB 데이터 삭제 (엔티티 객체로 삭제)
        postRepository.delete(post);
    }

    // HTML에서 파일명만 쏙 뽑아내는 도구 메서드
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
