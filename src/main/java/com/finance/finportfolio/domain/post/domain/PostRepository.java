package com.finance.finportfolio.domain.post.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    /*
     * SimpleJpaRepository 구현체 자동 생성
     * - save(), findById(), delete() 등 메서드 수행
     * - EntityManager 생성 - persist() merge()
     */
    // 메서드 이름 분석을 통한 JPQL을 생성 및 실행
}