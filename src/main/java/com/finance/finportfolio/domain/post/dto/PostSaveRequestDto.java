package com.finance.finportfolio.domain.post.dto;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.finance.finportfolio.domain.post.domain.Post;

import lombok.Builder;

// PostSaveRequestDto - Post 엔티티 Set 용 DTO
@Builder
public record PostSaveRequestDto(
                String author,
                String title,
                String content) {

        // DTO -> Entity 변환 (DB 저장용)
        public Post toEntity() {

                // jsoup 커스텀 설정 본문용(utext)
                Safelist customList = Safelist.relaxed()
                                .addAttributes("img", "style", "alt", "width", "height") // 이미지 관련 속성 허용
                                .addTags("hr", "br"); // 가로줄, 줄바꿈 명시적 허용

                String cleanAuthor = (author == null) ? "" : Jsoup.clean(author, Safelist.none());
                String cleanTitle = (title == null) ? "" : Jsoup.clean(title, Safelist.none());
                String cleanContent = (content == null) ? "" : Jsoup.clean(content, customList);

                return Post.builder()
                                .author(cleanAuthor)
                                .title(cleanTitle)
                                .content(cleanContent) // Jsoup XSS 살균
                                .build();
        }
}