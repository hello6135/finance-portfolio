package com.finance.finportfolio.domain.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.domain.post.service.PostService;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Mock
    private PostService postService;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("대시보드 요약 정보 조회 성공")
    void getDashboardSummary_Success() {
        // given
        long expectedPostCount = 15L;
        long expectedImageCount = 30L;
        long expectedMemberCount = 5L;

        given(postService.getTotalPostCount()).willReturn(expectedPostCount);
        given(postService.s3ObjectCount()).willReturn(expectedImageCount);
        given(memberService.getTotalMemberCount()).willReturn(expectedMemberCount);

        // when
        AdminResponseDto result = adminService.getDashboardSummary();

        // then
        assertThat(result).isNotNull();
        assertThat(result.totalPostCount()).isEqualTo(expectedPostCount);
        assertThat(result.totalImageCount()).isEqualTo(expectedImageCount);
        assertThat(result.totalMemberCount()).isEqualTo(expectedMemberCount);

        // verify (의존 객체 메서드 호출 여부 검증)
        then(postService).should(times(1)).getTotalPostCount();
        then(postService).should(times(1)).s3ObjectCount();
        then(memberService).should(times(1)).getTotalMemberCount();
    }
}