package com.finance.finportfolio.domain.admin.service;

import org.springframework.stereotype.Service;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.member.service.MemberService;
import com.finance.finportfolio.domain.post.service.PostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final PostService postService;
    private final MemberService memberService;

    public AdminResponseDto getDashboardSummary() {
        long totalPostCount = postService.getTotalPostCount();
        long totalImageCount = postService.s3ObjectCount();
        long totalMemberCount = memberService.getTotalMemberCount();

        return new AdminResponseDto(totalPostCount, totalImageCount, totalMemberCount);
    }
}
