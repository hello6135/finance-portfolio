package com.finance.finportfolio.global.config;

import com.finance.finportfolio.domain.category.controller.CategoryController;
import com.finance.finportfolio.domain.category.service.CategoryService;
import com.finance.finportfolio.domain.member.controller.MemberController;
import com.finance.finportfolio.domain.member.controller.TokenCookieManager;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.domain.post.controller.PostController;
import com.finance.finportfolio.global.security.filter.CloudFrontHeaderFilter;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ PostController.class, MemberController.class, CategoryController.class })
@Import(SecurityConfig.class)
@ActiveProfiles("default")
class SecurityConfigDefaultTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.finance.finportfolio.domain.post.service.PostService postService;
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

    @Test
    @DisplayName("default 프로파일: contains('local')=false, isEmpty()=false, contains('default')=true → localhost:3000 허용")
    void cors_DefaultProfile_AllowsLocalhost() throws Exception {
        // contains("local")=false → 단락 안 됨
        // isEmpty()=false → 단락 안 됨
        // contains("default")=true → isLocalDevelopment=true 분기 진입
        mockMvc.perform(options("/api/posts/list")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}