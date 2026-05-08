package com.finance.finportfolio.domain.post.dto;

import java.time.LocalDateTime;
import java.util.Optional;

import com.finance.finportfolio.domain.category.entity.Category;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.post.entity.Post;

// PostResponseDto - Post 엔티티 Get용 DTO
public record PostResponseDto(
                Long id,
                String categoryName,
                String author,
                String title,
                String content,
                boolean hasImage,
                boolean isOwner,
                LocalDateTime createdAt) {
        // 원본 호출용
        public static PostResponseDto from(Post post) {
                return PostResponseDto.ofForJsoup(post, post.getContent(), null);
        }

        // 살균 Content 호출용
        public static PostResponseDto ofForJsoup(Post post, String processedContent, String currentLoginId) {
                boolean ownerCheck = Optional.ofNullable(post.getAuthor())
                                .map(Member::getLoginId)
                                .map(loginId -> loginId.equals(currentLoginId))
                                .orElse(false);

                return new PostResponseDto(
                                post.getId(),
                                Optional.ofNullable(post.getCategory())
                                                .map(Category::getName)
                                                .orElse("미분류"),
                                Optional.ofNullable(post.getAuthor())
                                                .map(Member::getNickname)
                                                .orElse("익명"),
                                post.getTitle(),
                                processedContent,
                                post.isHasImage(),
                                ownerCheck,
                                post.getCreatedAt());
        }
}