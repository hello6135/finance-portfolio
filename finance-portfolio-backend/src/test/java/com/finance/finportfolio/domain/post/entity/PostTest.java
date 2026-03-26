package com.finance.finportfolio.domain.post.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PostTest {

    @Test
    @DisplayName("빌더를 통해 Post 객체가 정상적으로 생성되어야 한다")
    void createPostWithBuilder() {
        // given
        String author = "tester";
        String title = "테스트 제목";
        String content = "<p>테스트 내용</p>";
        boolean hasImage = true;

        // when
        Post post = Post.builder()
                .author(author)
                .title(title)
                .content(content)
                .hasImage(hasImage)
                .build();

        // then
        assertThat(post.getAuthor()).isEqualTo(author);
        assertThat(post.getTitle()).isEqualTo(title);
        assertThat(post.getContent()).isEqualTo(content);
        assertThat(post.isHasImage()).isTrue();
    }

    @Test
    @DisplayName("update 메서드 호출 시 엔티티의 상태가 변경되어야 한다")
    void updatePost() {
        // given
        Post post = Post.builder()
                .author("tester")
                .title("기존 제목")
                .content("기존 내용")
                .hasImage(false)
                .build();

        String newTitle = "수정된 제목";
        String newContent = "<h1>수정된 내용</h1>";
        boolean newHasImage = true;

        // when
        post.update(newTitle, newContent, newHasImage);

        // then
        assertThat(post.getTitle()).isEqualTo(newTitle);
        assertThat(post.getContent()).isEqualTo(newContent);
        assertThat(post.isHasImage()).isTrue();
        // author는 수정 로직에 없으므로 유지되어야 함
        assertThat(post.getAuthor()).isEqualTo("tester");
    }
}