package com.finance.finportfolio.domain.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.FilterType;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.admin.service.AdminService;
import com.finance.finportfolio.domain.admin.service.AwsHealthCheckService;
import com.finance.finportfolio.domain.member.dto.MemberAdminResponseDto;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.global.security.filter.JwtAuthenticationFilter;

@WebMvcTest(value = AdminController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private AwsHealthCheckService awsHealthCheckService;

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

    @Test
    @DisplayName("관리자 권한으로 AWS 헬스체크 조회 성공")
    @WithMockUser(roles = "ADMIN")
    void getAwsStatus_Success_WhenAdmin() throws Exception {
        // given
        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("s3Status", "UP");
        mockResponse.put("ec2Status", "RUNNING");

        given(awsHealthCheckService.checkAwsStatus()).willReturn(mockResponse);

        // when & then
        mockMvc.perform(get("/api/admin/awsHealth")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.s3Status").value("UP"))
                .andExpect(jsonPath("$.ec2Status").value("RUNNING"));
    }

    @Test
    @DisplayName("권한이 없는 일반 사용자가 AWS 헬스체크 조회 시 403 Forbidden 반환")
    @WithMockUser(roles = "USER")
    void getAwsStatus_Forbidden_WhenUser() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/awsHealth")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증되지 않은 비회원이 AWS 헬스체크 조회 시 403 Forbidden 반환")
    void getAwsStatus_Unauthorized_WhenAnonymous() throws Exception {
        // when & then
        mockMvc.perform(get("/api/admin/awsHealth")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 권한으로 전체 회원 페이징 조회 성공")
    @WithMockUser(roles = "ADMIN")
    void getMemberList_Success_WhenAdmin() throws Exception {
        // given
        MemberAdminResponseDto memberDto = new MemberAdminResponseDto(1L, "user@test.com", "홍길동", Role.ADMIN, false, 0);
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id"));
        Page<MemberAdminResponseDto> mockPage = new PageImpl<>(Collections.singletonList(memberDto), pageable, 1);

        given(memberService.getMembersForAdmin(any(Pageable.class))).willReturn(mockPage);

        // when & then
        mockMvc.perform(get("/api/admin/members")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "id,desc")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].loginId").value("user@test.com"))
                .andExpect(jsonPath("$.content[0].nickname").value("홍길동"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("권한이 없는 일반 사용자가 전체 회원 조회 시 403 Forbidden 반환")
    @WithMockUser(roles = "USER")
    void getMemberList_Forbidden_WhenUser() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 권한으로 회원 정지 상태 변경 토글 성공 및 204 No Content 반환")
    @WithMockUser(roles = "ADMIN")
    void toggleBanStatus_Success_WhenAdmin() throws Exception {
        // given
        Long memberId = 1L;
        boolean shouldBan = true;
        willDoNothing().given(memberService).updateBanStatus(eq(memberId), eq(shouldBan));

        // when & then
        mockMvc.perform(patch("/api/admin/members/{memberId}/ban", memberId)
                .param("status", String.valueOf(shouldBan))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent()); // 204 No Content
    }

    @Test
    @DisplayName("권한이 없는 일반 사용자가 회원 정지 토글 시 403 Forbidden 반환")
    @WithMockUser(roles = "USER")
    void toggleBanStatus_Forbidden_WhenUser() throws Exception {
        mockMvc.perform(patch("/api/admin/members/1/ban")
                .param("status", "true")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 권한으로 회원 계정 잠금 수동 해제 성공 및 204 No Content 반환")
    @WithMockUser(roles = "ADMIN")
    void unlockMember_Success_WhenAdmin() throws Exception {
        // given
        Long memberId = 1L;
        willDoNothing().given(memberService).releaseMemberLock(eq(memberId));

        // when & then
        mockMvc.perform(post("/api/admin/members/{memberId}/unlock", memberId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("권한이 없는 일반 사용자가 회원 임시 잠금 해제 요청 시 403 Forbidden 반환")
    @WithMockUser(roles = "USER")
    void unlockMember_Forbidden_WhenUser() throws Exception {
        mockMvc.perform(post("/api/admin/members/1/unlock")
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}