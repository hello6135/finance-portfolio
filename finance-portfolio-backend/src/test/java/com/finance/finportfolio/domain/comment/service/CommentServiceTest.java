package com.finance.finportfolio.domain.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.finance.finportfolio.domain.comment.dto.CommentRequestDto;
import com.finance.finportfolio.domain.comment.dto.CommentResponseDto;
import com.finance.finportfolio.domain.comment.entity.Comment;
import com.finance.finportfolio.domain.comment.repository.CommentRepository;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.domain.post.entity.Post;
import com.finance.finportfolio.domain.post.repository.PostRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CommentService commentService;

    private Member author;
    private Post post;

    @BeforeEach
    void setUp() {
        author = Member.builder()
                .loginId("testuser")
                .nickname("Tester")
                .password("password")
                .build();
        ReflectionTestUtils.setField(author, "id", 1L);

        post = Post.builder()
                .title("Test Post")
                .content("Content")
                .author(author)
                .build();
        ReflectionTestUtils.setField(post, "id", 100L);
    }

    @Nested
    @DisplayName("댓글 생성 테스트")
    class CreateComment {

        @Test
        @DisplayName("성공적으로 루트 댓글을 생성한다")
        void saveRootCommentSuccessTest() {
            // given
            CommentRequestDto requestDto = new CommentRequestDto(100L, null, "Hello Comment");
            Comment savedComment = Comment.builder()
                    .post(post)
                    .author(author)
                    .content("Hello Comment")
                    .build();
            ReflectionTestUtils.setField(savedComment, "id", 500L);

            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            given(memberRepository.findByLoginId("testuser")).willReturn(Optional.of(author));
            given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

            // when
            Long resultId = commentService.saveComment(requestDto, "testuser");

            // then
            assertThat(resultId).isEqualTo(500L);
            verify(commentRepository, times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("성공적으로 대댓글을 생성한다")
        void saveChildCommentSuccessTest() {
            // given
            Comment parentComment = Comment.builder()
                    .post(post)
                    .author(author)
                    .content("Parent Content")
                    .build();
            ReflectionTestUtils.setField(parentComment, "id", 500L);

            CommentRequestDto requestDto = new CommentRequestDto(100L, 500L, "Child Comment");
            Comment savedChild = Comment.builder()
                    .post(post)
                    .author(author)
                    .content("Child Comment")
                    .parent(parentComment)
                    .build();
            ReflectionTestUtils.setField(savedChild, "id", 501L);

            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            given(memberRepository.findByLoginId("testuser")).willReturn(Optional.of(author));
            given(commentRepository.findById(500L)).willReturn(Optional.of(parentComment));
            given(commentRepository.save(any(Comment.class))).willReturn(savedChild);

            // when
            Long resultId = commentService.saveComment(requestDto, "testuser");

            // then
            assertThat(resultId).isEqualTo(501L);
            verify(commentRepository, times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("대댓글의 대댓글(Depth 2 이상)이 작성될 경우 부모를 최상위 부모(Root)로 평탄화한다")
        void saveGrandchildCommentFlatteningTest() {
            // given: Root 댓글과 그 자식인 대댓글 준비
            Comment rootComment = Comment.builder()
                    .post(post)
                    .author(author)
                    .content("Root Comment")
                    .build();
            ReflectionTestUtils.setField(rootComment, "id", 500L);

            Comment parentComment = Comment.builder()
                    .post(post)
                    .author(author)
                    .content("Parent Content (Child of Root)")
                    .parent(rootComment)
                    .build();
            ReflectionTestUtils.setField(parentComment, "id", 501L);

            // 대댓글(parentComment)에 다시 답글을 달려고 시도 (Grandchild)
            CommentRequestDto requestDto = new CommentRequestDto(100L, 501L, "Grandchild Comment");

            // 저장될 결과는 parent가 parentComment가 아닌 최상위 rootComment로 평탄화되어야 함
            Comment savedFlattenedChild = Comment.builder()
                    .post(post)
                    .author(author)
                    .content("Grandchild Comment")
                    .parent(rootComment) // parent가 rootComment로 교체됨!
                    .build();
            ReflectionTestUtils.setField(savedFlattenedChild, "id", 502L);

            given(postRepository.findById(100L)).willReturn(Optional.of(post));
            given(memberRepository.findByLoginId("testuser")).willReturn(Optional.of(author));
            given(commentRepository.findById(501L)).willReturn(Optional.of(parentComment));
            given(commentRepository.save(argThat(comment -> 
                comment.getParent() != null && comment.getParent().getId().equals(500L)
            ))).willReturn(savedFlattenedChild);

            // when
            Long resultId = commentService.saveComment(requestDto, "testuser");

            // then
            assertThat(resultId).isEqualTo(502L);
            verify(commentRepository, times(1)).save(any(Comment.class));
        }

        @Test
        @DisplayName("게시글이 존재하지 않으면 예외가 발생한다")
        void saveCommentPostNotFoundExceptionTest() {
            // given
            CommentRequestDto requestDto = new CommentRequestDto(999L, null, "Hello Comment");
            given(postRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> commentService.saveComment(requestDto, "testuser"))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("게시글을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("댓글 조회 및 구조화 테스트")
    class ReadComments {

        @Test
        @DisplayName("게시글 번호로 계층화된 댓글 목록을 올바르게 트리 구조화하여 반환한다")
        void getCommentsByPostHierarchyTest() {
            // given
            Comment root1 = Comment.builder().post(post).author(author).content("Root 1").build();
            ReflectionTestUtils.setField(root1, "id", 1L);

            Comment root2 = Comment.builder().post(post).author(author).content("Root 2").build();
            ReflectionTestUtils.setField(root2, "id", 2L);

            Comment child1 = Comment.builder().post(post).author(author).content("Child 1").parent(root1).build();
            ReflectionTestUtils.setField(child1, "id", 3L);

            given(commentRepository.findByPostIdOrderByIdAsc(100L)).willReturn(List.of(root1, root2, child1));

            // when
            List<CommentResponseDto> result = commentService.getCommentsByPost(100L);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).id()).isEqualTo(3L);
            assertThat(result.get(1).id()).isEqualTo(2L);
            assertThat(result.get(1).children()).isEmpty();
        }
    }

    @Nested
    @DisplayName("댓글 수정 및 삭제 테스트")
    class UpdateAndDeleteComment {

        @Test
        @DisplayName("댓글 내용을 정상적으로 수정한다")
        void updateCommentSuccessTest() {
            // given
            Comment comment = Comment.builder().post(post).author(author).content("Old Content").build();
            ReflectionTestUtils.setField(comment, "id", 1L);

            CommentRequestDto updateRequest = new CommentRequestDto(100L, null, "New Content");
            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when
            commentService.updateComment(1L, updateRequest);

            // then
            assertThat(comment.getContent()).isEqualTo("New Content");
        }

        @Test
        @DisplayName("댓글을 정상적으로 삭제한다")
        void deleteCommentSuccessTest() {
            // given
            Comment comment = Comment.builder().post(post).author(author).content("Comment to delete").build();
            ReflectionTestUtils.setField(comment, "id", 1L);

            given(commentRepository.findById(1L)).willReturn(Optional.of(comment));

            // when
            commentService.deleteComment(1L);

            // then
            verify(commentRepository, times(1)).delete(comment);
        }
    }
}
