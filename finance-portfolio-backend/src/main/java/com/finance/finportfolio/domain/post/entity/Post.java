package com.finance.finportfolio.domain.post.entity;

import java.time.LocalDateTime;

// JPA - 시간 자동화
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.finance.finportfolio.domain.category.entity.Category;

// JPA
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners; //JPA 시간용
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
// Lombok
import lombok.Getter;
import lombok.NoArgsConstructor;

// Post - 게시글 엔티티 구조
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 빌더를 쓰려면 기본 생성자 필수, 외부호출 금지
@EntityListeners(AuditingEntityListener.class) // 시간 감시자
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Category DB와 JOIN
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private String author;

    private String title;
    @Column(columnDefinition = "TEXT") // CKEditor : HTML 문자열로 저장
    private String content;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean hasImage = false;

    @CreatedDate // 생성 시 자동 저장
    @Column(updatable = false) // 생성 후 수정 불가
    private LocalDateTime createdAt;

    @Builder
    private Post(Category category, String author, String title, String content, boolean hasImage) {
        this.category = category;
        this.author = author;
        this.title = title;
        this.content = content;
        this.hasImage = hasImage;
    }

    public void update(Category category, String title, String content, boolean hasImage) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.hasImage = hasImage;
    }
}