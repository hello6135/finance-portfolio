package com.finance.finportfolio.domain.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.comment.dto.CommentRequestDto;
import com.finance.finportfolio.domain.comment.dto.CommentResponseDto;
import com.finance.finportfolio.domain.comment.service.CommentService;
import com.finance.finportfolio.global.security.filter.JwtAuthenticationFilter;

@WebMvcTest(value = CommentController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CommentService commentService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/comments/**").permitAll()
                            .anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @DisplayName("특정 게시글의 계층화된 댓글 목록을 정상 조회한다")
    void getComments_Success() throws Exception {
        // given
        LocalDateTime fixedTime = LocalDateTime.of(2026, Month.JUNE, 6, 23, 0, 0);
        CommentResponseDto child = new CommentResponseDto(2L, "Child", "childUser", "Child Content", fixedTime, List.of());
        CommentResponseDto parent = new CommentResponseDto(1L, "Parent", "parentUser", "Parent Content", fixedTime, List.of(child));
        given(commentService.getCommentsByPost(100L)).willReturn(List.of(parent));

        // when & then
        mockMvc.perform(get("/api/comments/post/100")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].authorNickname").value("Parent"))
                .andExpect(jsonPath("$[0].children[0].id").value(2))
                .andExpect(jsonPath("$[0].children[0].authorNickname").value("Child"));
    }

    @Test
    @DisplayName("인증된 사용자가 새로운 댓글을 등록한다")
    @WithMockUser(username = "testuser")
    void createComment_Success() throws Exception {
        // given
        CommentRequestDto requestDto = new CommentRequestDto(100L, null, "Hello Content");
        given(commentService.saveComment(any(CommentRequestDto.class), any())).willReturn(500L);

        // when & then
        mockMvc.perform(post("/api/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$").value(500));
    }

    @Test
    @DisplayName("댓글 내용을 정상적으로 수정한다")
    void updateComment_Success() throws Exception {
        // given
        CommentRequestDto requestDto = new CommentRequestDto(100L, null, "Updated Content");
        willDoNothing().given(commentService).updateComment(eq(1L), any(CommentRequestDto.class));

        // when & then
        mockMvc.perform(patch("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1));
    }

    @Test
    @DisplayName("댓글을 정상적으로 삭제한다")
    void deleteComment_Success() throws Exception {
        // given
        willDoNothing().given(commentService).deleteComment(1L);

        // when & then
        mockMvc.perform(delete("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
