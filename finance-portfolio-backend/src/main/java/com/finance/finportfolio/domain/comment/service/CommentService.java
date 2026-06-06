package com.finance.finportfolio.domain.comment.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.comment.dto.CommentRequestDto;
import com.finance.finportfolio.domain.comment.dto.CommentResponseDto;
import com.finance.finportfolio.domain.comment.entity.Comment;
import com.finance.finportfolio.domain.comment.repository.CommentRepository;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.domain.post.entity.Post;
import com.finance.finportfolio.domain.post.repository.PostRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("commentService")
@Transactional
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public boolean isCommentOwner(Long id, String loginId) {
        if (id == null) {
            return false;
        }
        return commentRepository.findById(id)
                .map(comment -> comment.getAuthor() != null && comment.getAuthor().getLoginId().equals(loginId))
                .orElse(false);
    }

    public Long saveComment(CommentRequestDto requestDto, String loginId) {
        Post post = postRepository.findById(requestDto.postId())
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다."));

        Member author = memberRepository.findByLoginId(loginId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        Comment parent = null;
        if (requestDto.parentId() != null) {
            parent = commentRepository.findById(requestDto.parentId())
                    .orElseThrow(() -> new EntityNotFoundException("부모 댓글을 찾을 수 없습니다."));

            // 대댓글의 대댓글(Depth 2 이상)이 발생하는 경우,
            // 자동으로 최상위 부모(Root Comment, 즉 Depth 0)로 매핑하여 무한 뎁스 방지 및 최대 2단계(0, 1)로 평탄화(Flatten).
            if (parent.getParent() != null) {
                parent = parent.getParent();
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(requestDto.content())
                .parent(parent)
                .build();

        return commentRepository.save(comment).getId();
    }

    public void updateComment(Long id, CommentRequestDto requestDto) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));
        comment.update(requestDto.content());
    }

    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponseDto> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByIdAsc(postId);

        List<CommentResponseDto> rootComments = new ArrayList<>();
        Map<Long, List<CommentResponseDto>> childrenMap = new HashMap<>();
        Map<Long, CommentResponseDto> dtoMap = new HashMap<>();

        // Create initial DTOs and populate parent-child relationships
        for (Comment comment : comments) {
            List<CommentResponseDto> childrenList = new ArrayList<>();
            childrenMap.put(comment.getId(), childrenList);

            CommentResponseDto dto = CommentResponseDto.of(comment, childrenList);
            dtoMap.put(comment.getId(), dto);

            if (comment.getParent() == null) {
                rootComments.add(dto);
            } else {
                Long parentId = comment.getParent().getId();
                if (childrenMap.containsKey(parentId)) {
                    childrenMap.get(parentId).add(dto);
                }
            }
        }

        return rootComments;
    }
}
