package com.finance.finportfolio.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.finportfolio.domain.category.controller.CategoryController;
import com.finance.finportfolio.domain.category.dto.CategoryRequestDto;
import com.finance.finportfolio.domain.category.service.CategoryService;
import com.finance.finportfolio.domain.member.controller.MemberController;
import com.finance.finportfolio.domain.member.controller.TokenCookieManager;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.domain.post.controller.PostController;
import com.finance.finportfolio.domain.post.dto.PostResponseDto;
import com.finance.finportfolio.global.security.filter.CloudFrontHeaderFilter;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

/**
 * SecurityConfig의 authenticationEntryPoint 동작 검증 테스트
 *
 * 검증 대상:
 * 1. 인증 없이 보호된 리소스 접근 시 401 + UNAUTHORIZED JSON 반환
 * 2. 공개 허용 경로(GET /api/posts/**)는 인증 없이 통과
 * 3. 인증 후 보호된 리소스 접근 가능
 */
@WebMvcTest({ PostController.class, MemberController.class, CategoryController.class })
@Import(SecurityConfig.class)
class SecurityConfigTest {

        @Autowired
        private MockMvc mockMvc;

        // PostController 의존성 Mock
        @MockitoBean
        private com.finance.finportfolio.domain.post.service.PostService postService;

        // MemberController 타고 들어온 의존성
        @MockitoBean
        private TokenCookieManager tokenCookieManager;

        @MockitoBean
        private MemberService memberService;

        @MockitoBean
        private CategoryService categoryService;

        @MockitoBean
        private JwtTokenProvider jwtTokenProvider;

        @MockitoBean
        private AuthenticationManager authenticationManager;

        @MockitoBean
        private CloudFrontHeaderFilter cloudFrontHeaderFilter;

        @Autowired
        private ObjectMapper objectMapper;

        // CloudFrontHeaderFilter
        @Test
        @DisplayName("CloudFrontHeaderFilter가 빈으로 존재하면 필터 체인에서 실행되어야 한다")
        void cloudFrontFilter_ShouldExecute_WhenBeanExists() throws Exception {
                // given: 필터가 호출될 때 다음 필터로 넘어가도록 설정 (가짜 동작 정의)
                // doAnswer를 사용하여 실제 필터의 doFilterInternal 로직을 흉내냅니다.
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                // when: 어떤 요청이든 수행
                mockMvc.perform(get("/api/posts/list"))
                                .andExpect(status().isOk());

                // then: 필터가 최소 1회 호출되었는지 검증
                // addFilterBefore로 등록되었으므로 요청 처리 과정에서 반드시 거쳐야 함
                verify(cloudFrontHeaderFilter, atLeastOnce()).doFilter(any(), any(), any());
        }

        @Test
        @DisplayName("CloudFront 커스텀 헤더가 없으면 403 또는 필터에서 정의한 에러를 반환해야 한다")
        void cloudFrontFilter_ShouldReject_WhenHeaderIsMissing() throws Exception {
                // given: 헤더가 없을 때 필터가 403을 응답하도록 Mock 설정 (필터의 실제 로직에 맞게 조정)
                doAnswer(invocation -> {
                        jakarta.servlet.http.HttpServletResponse response = invocation.getArgument(1);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(
                                argThat(request -> ((jakarta.servlet.http.HttpServletRequest) request)
                                                .getHeader("X-Custom-Access-Key") == null),
                                any(),
                                any());

                // when & then
                mockMvc.perform(get("/api/posts/list")) // 헤더 없이 요청
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("로컬 환경에서는 localhost:3000의 CORS 요청을 허용해야 한다")
        void corsAllowedInLocalProfile() throws Exception {
                mockMvc.perform(options("/api/posts/list")
                                .header("Origin", "http://localhost:3000")
                                .header("Access-Control-Request-Method", "GET"))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }

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
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                mockMvc.perform(get("/api/admin/some-resource")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized()); // Custom EntryPoint 작동 확인
        }

        // ── authenticationEntryPoint 핵심 검증 ────────────────────

        @Test
        @DisplayName("인증 없이 보호된 엔드포인트에 접근하면 401과 UNAUTHORIZED JSON을 반환한다")
        void accessProtectedResource_WithoutAuth_Returns401WithJson() throws Exception {
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

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
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                mockMvc.perform(delete("/api/posts/1"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(content().contentType("application/json;charset=UTF-8"))
                                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        @Test
        @DisplayName("인증 없이 PATCH 요청을 하면 401과 UNAUTHORIZED JSON을 반환한다")
        void patchPost_WithoutAuth_Returns401WithJson() throws Exception {
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

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
                Page<PostResponseDto> emptyPage = new PageImpl<>(List.of());
                // postService.getPostList(page, size)는 기본 Mock → 빈 리스트를 포함하는 Page 객체 반환
                org.mockito.BDDMockito.given(postService.getPostList(0, 10, 1L))
                                .willReturn(emptyPage);

                mockMvc.perform(get("/api/posts/list") // 엔드포인트 경로 확인 (/list 추가 여부)
                                .param("page", "0")
                                .param("size", "10"))
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
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                mockMvc.perform(post("/api/posts")
                                .contentType("application/json")
                                .content("{}"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(content().contentType("application/json;charset=UTF-8"));
        }

        @Test
        @DisplayName("401 응답 바디에 error 필드가 UNAUTHORIZED 값으로 포함되어야 한다")
        void unauthorizedResponse_Body_ContainsErrorField() throws Exception {
                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                mockMvc.perform(delete("/api/posts/1"))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        }

        // 카테고리 api 관련

        @Test
        @DisplayName("GET /api/category 는 인증 없이도 200을 반환한다 (permitAll)")
        void getCategories_NoAuth_Success() throws Exception {
                mockMvc.perform(get("/api/category"))
                                .andDo(print())
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST: 일반 유저(USER)는 카테고리를 생성할 수 없어야 한다 (403 Forbidden)")
        void saveCategory_UserRole_Forbidden() throws Exception {
                CategoryRequestDto dto = new CategoryRequestDto("Investment", 1);
                String json = objectMapper.writeValueAsString(dto);

                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                mockMvc.perform(post("/api/category")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST: 관리자 권한이면 서비스 호출까지 도달해야 한다")
        void saveCategory_Success() throws Exception {
                // Service가 어떤 DTO를 받든 1L을 리턴하도록 가짜 설정 (500 에러 방지)
                given(categoryService.save(any())).willReturn(1L);

                CategoryRequestDto dto = new CategoryRequestDto("Stock", 1);
                String json = objectMapper.writeValueAsString(dto);

                mockMvc.perform(post("/api/category")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk()); // 이제 서비스 로직 에러 없이 200이 뜹니다.
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST: 관리자(ADMIN)는 카테고리를 생성할 수 있어야 한다 (200 OK)")
        void saveCategory_AdminRole_Success() throws Exception {
                CategoryRequestDto dto = new CategoryRequestDto("Stock", 1);
                String json = objectMapper.writeValueAsString(dto);

                mockMvc.perform(post("/api/category")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("PATCH: 일반 유저(USER)는 카테고리를 수정할 수 없어야 한다 (403 Forbidden)")
        void updateCategory_UserRole_Forbidden() throws Exception {
                CategoryRequestDto dto = new CategoryRequestDto("Updated Name", 2);
                String json = objectMapper.writeValueAsString(dto);

                doAnswer(invocation -> {
                        jakarta.servlet.FilterChain chain = invocation.getArgument(2);
                        chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                        return null;
                }).when(cloudFrontHeaderFilter).doFilter(any(), any(), any());

                mockMvc.perform(patch("/api/category/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE: 관리자(ADMIN)는 카테고리를 삭제할 수 있어야 한다 (200 OK)")
        void deleteCategory_AdminRole_Success() throws Exception {
                mockMvc.perform(delete("/api/category/1"))
                                .andDo(print())
                                .andExpect(status().isOk());
        }

}

@SpringBootTest
@ActiveProfiles("local")
class SecurityConfigBeanTest {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Test
        @DisplayName("AuthenticationManager 빈이 정상 생성된다")
        void authenticationManagerBean_ShouldBeCreated() {
                assertThat(authenticationManager).isNotNull();
        }
}