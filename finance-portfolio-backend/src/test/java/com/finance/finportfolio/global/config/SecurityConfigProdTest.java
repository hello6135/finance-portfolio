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
@ActiveProfiles("prod")
class SecurityConfigProdTest {

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
    @DisplayName("prod 프로파일: 셋 다 false → else 분기 → setAllowedOrigins(List.of()) 실행")
    void cors_ProdProfile_BlocksAllOrigins() throws Exception {
        mockMvc.perform(options("/api/posts/list")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}