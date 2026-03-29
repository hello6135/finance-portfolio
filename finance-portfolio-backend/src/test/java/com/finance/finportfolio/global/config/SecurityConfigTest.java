package com.finance.finportfolio.global.config;

import com.finance.finportfolio.domain.member.controller.MemberController;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.domain.post.controller.PostController;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*; // 이 줄이 print()를 가능하게 합니다.

/**
 * SecurityConfig의 authenticationEntryPoint 동작 검증 테스트
 *
 * 검증 대상:
 * 1. 인증 없이 보호된 리소스 접근 시 401 + UNAUTHORIZED JSON 반환
 * 2. 공개 허용 경로(GET /api/posts/**)는 인증 없이 통과
 * 3. 인증 후 보호된 리소스 접근 가능
 */
@WebMvcTest({ PostController.class, MemberController.class })
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    // PostController 의존성 Mock
    @MockBean
    private com.finance.finportfolio.domain.post.service.PostService postService;

    @MockBean
    private MemberService memberService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuthenticationManager authenticationManager;

    @Test
    @DisplayName("CORS Preflight 요청(OPTIONS)은 인증 없이 200 OK를 반환해야 한다")
    void corsPreflightTest() throws Exception {
        mockMvc.perform(options("/api/posts")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    @DisplayName("로그인 경로는 permitAll이므로 접근 시 401이 발생하지 않는다")
    void loginPath_ShouldNotReturn401() throws Exception {
        mockMvc.perform(post("/api/member/login") // POST로 변경
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\", \"password\":\"1234\"}")) // 가짜 바디
                .andDo(print())
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));
    }

    @Test
    @DisplayName("인증이 필요한 경로에 토큰 없이 접근하면 401 에러가 발생해야 한다")
    void authenticatedPathFailTest() throws Exception {
        mockMvc.perform(get("/api/admin/some-resource")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); // Custom EntryPoint 작동 확인
    }

    // ── authenticationEntryPoint 핵심 검증 ────────────────────

    @Test
    @DisplayName("인증 없이 보호된 엔드포인트에 접근하면 401과 UNAUTHORIZED JSON을 반환한다")
    void accessProtectedResource_WithoutAuth_Returns401WithJson() throws Exception {
        // POST /api/posts 는 anyRequest().authenticated() 대상
        mockMvc.perform(post("/api/posts")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인증 없이 DELETE 요청을 하면 401과 UNAUTHORIZED JSON을 반환한다")
    void deletePost_WithoutAuth_Returns401WithJson() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인증 없이 PATCH 요청을 하면 401과 UNAUTHORIZED JSON을 반환한다")
    void patchPost_WithoutAuth_Returns401WithJson() throws Exception {
        mockMvc.perform(patch("/api/posts/1")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    // ── 공개 허용 경로 검증 ────────────────────────────────────

    @Test
    @DisplayName("GET /api/posts 는 인증 없이도 200을 반환한다 (permitAll)")
    void getAllPosts_WithoutAuth_Returns200() throws Exception {
        // postService.getAllPosts()는 기본 Mock → 빈 리스트 반환
        org.mockito.BDDMockito.given(postService.getAllPosts())
                .willReturn(java.util.List.of());

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/posts/{id} 는 인증 없이도 접근 가능하다 (permitAll)")
    void getPostById_WithoutAuth_IsPermitted() throws Exception {
        // 존재하지 않는 ID → 서비스에서 예외 발생하더라도 인증 문제는 아님
        // 여기서는 401이 아닌 것만 확인 (404 or 400은 서비스 레이어 동작)
        mockMvc.perform(get("/api/posts/999"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus())
                        .isNotEqualTo(401));
    }

    // ── 응답 형식 검증 ─────────────────────────────────────────

    @Test
    @DisplayName("401 응답의 Content-Type은 application/json;charset=UTF-8이어야 한다")
    void unauthorizedResponse_ContentType_IsJson() throws Exception {
        mockMvc.perform(post("/api/posts")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"));
    }

    @Test
    @DisplayName("401 응답 바디에 error 필드가 UNAUTHORIZED 값으로 포함되어야 한다")
    void unauthorizedResponse_Body_ContainsErrorField() throws Exception {
        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}