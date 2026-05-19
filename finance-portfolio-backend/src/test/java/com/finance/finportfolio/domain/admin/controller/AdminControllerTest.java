package com.finance.finportfolio.domain.admin.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.FilterType;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.admin.service.AdminService;
import com.finance.finportfolio.global.security.filter.JwtAuthenticationFilter;

@WebMvcTest(value = AdminController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth
                            // 실제 운영 환경의 인가 규칙과 동일하게 매핑
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().permitAll());
            return http.build();
        }
    }

    @Test
    @DisplayName("관리자 권한으로 대시보드 통계 조회 성공")
    @WithMockUser(roles = "ADMIN") // ROLE_ADMIN 권한을 가진 가상의 사용자 생성
    void getCounts_Success_WhenAdmin() throws Exception {
        // given
        AdminResponseDto mockResponse = new AdminResponseDto(128L, 45L, 12L);
        given(adminService.getDashboardSummary()).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/admin/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPostCount").value(128))
                .andExpect(jsonPath("$.totalImageCount").value(45))
                .andExpect(jsonPath("$.totalMemberCount").value(12));
    }

    @Test
    @DisplayName("권한이 없는 일반 사용자가 대시보드 조회 시 403 Forbidden 반환")
    @WithMockUser(roles = "USER") // ROLE_USER 권한을 가진 가상의 사용자 생성
    void getCounts_Forbidden_WhenUser() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // SecurityConfig 설정에 의해 차단됨
    }

    @Test
    @DisplayName("인증되지 않은 비회원이 대시보드 조회 시 401 Unauthorized 또는 403 Forbidden 반환")
    void getCounts_Unauthorized_WhenAnonymous() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden()); // Spring Security 기본 익명 사용자 차단 정책
    }
}