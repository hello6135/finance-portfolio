package com.finance.finportfolio.domain.comment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.finance.finportfolio.domain.comment.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "parent"})
    List<Comment> findByPostIdOrderByIdAsc(Long postId);
}
