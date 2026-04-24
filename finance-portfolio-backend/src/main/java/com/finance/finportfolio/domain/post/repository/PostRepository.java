package com.finance.finportfolio.domain.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import com.finance.finportfolio.domain.post.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {
    /*
     * SimpleJpaRepository 구현체 자동 생성
     * - save(), findById(), delete() 등 메서드 수행
     * - EntityManager 생성 - persist() merge()
     */
    // 메서드 이름 분석을 통한 JPQL을 생성 및 실행

    @NonNull
    @EntityGraph(attributePaths = { "category" }) // N+1 방지: category를 함께 fetch join해서 가져옴
    Page<Post> findAll(@NonNull Pageable pageable);

    // 카테고리 ID로 페이징 조회 (N+1 방지 포함)
    @EntityGraph(attributePaths = { "category" })
    Page<Post> findByCategoryId(Long categoryId, Pageable pageable);
}