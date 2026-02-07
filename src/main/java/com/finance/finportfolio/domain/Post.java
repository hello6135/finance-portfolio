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

// Post - 게시글 엔티티 구조
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
    private String author;

    private String title;
    @Column(columnDefinition = "TEXT") // CKEditor : HTML 문자열로 저장
    private String content;

    @CreatedDate // 생성 시 자동 저장
    @Column(updatable = false) // 생성 후 수정 불가
    private LocalDateTime createdAt;
}