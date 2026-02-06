package com.finance.finportfolio.domain;

import java.time.LocalDateTime;

// JPA - 시간 자동화
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

// JPA
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners; //JPA 시간용
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
// Lombok
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 빌더를 쓰려면 기본 생성자 필수, 외부호출 금지
@AllArgsConstructor // 빌더를 쓰려면 전체 생성자도 필수
@Builder // 빌더
@EntityListeners(AuditingEntityListener.class) // 시간 감시자
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // 프로젝트명/활동명
    @Column(columnDefinition = "TEXT")
    private String content; // 상세 설명
    private String author; // 작성자
    // private String imageUrl; // CKEditor방식으로 변경

    // private LocalDateTime createdAt = LocalDateTime.now(); // JPA방식으로 변경
    @CreatedDate // 생성 시 자동 저장
    @Column(updatable = false) // 생성 후 수정 불가 (금융 보안 원칙)
    private LocalDateTime createdAt;
}