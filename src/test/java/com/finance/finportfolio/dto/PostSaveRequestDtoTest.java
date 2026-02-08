package com.finance.finportfolio.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.finance.finportfolio.domain.Post;

class PostSaveRequestDtoTest {

    @Test
    @DisplayName("DTO를 엔티티로 변환 시 XSS 스크립트가 제거되어야 한다")
    void shouldCleanXssScriptWhenConvertingToEntity() {
        // given: 공격용 스크립트가 포함된 데이터
        String dirtyContent = "<script>alert('공격!')</script><p>안전한 내용</p>";
        PostSaveRequestDto dto = PostSaveRequestDto.builder()
                .author("테스터")
                .title("테스트 제목")
                .content(dirtyContent)
                .build();

        // when: 엔티티로 변환
        Post post = dto.toEntity();

        // then: 스크립트는 사라지고 안전한 태그만 남았는지 검증
        assertThat(post.getContent()).doesNotContain("<script>");
        assertThat(post.getContent()).contains("<p>안전한 내용</p>");
        System.out.println("살균 후 결과: " + post.getContent());
    }
}