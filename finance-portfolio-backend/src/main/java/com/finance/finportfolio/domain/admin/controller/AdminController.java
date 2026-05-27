package com.finance.finportfolio.domain.admin.controller;

import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.admin.service.AdminService;
import com.finance.finportfolio.domain.admin.service.AwsHealthCheckService;
import com.finance.finportfolio.domain.member.dto.MemberAdminResponseDto;
import com.finance.finportfolio.domain.member.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminService adminService;
    private final MemberService memberService;
    private final AwsHealthCheckService awsHealthCheckService;

    // 총 게시글, s3오브젝트, 총 회원 수 종합 전달
    @GetMapping("/summary")
    public ResponseEntity<AdminResponseDto> getCounts() {
        AdminResponseDto summary = adminService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }

    // AWS S3, EC2 상태 전달
    @GetMapping("/awsHealth")
    public ResponseEntity<Map<String, Object>> getAwsStatus() {
        Map<String, Object> response = awsHealthCheckService.checkAwsStatus();

        return ResponseEntity.ok(response);
    }

    // 전체 회원 조회(페이징)
    @GetMapping("/members")
    public ResponseEntity<Page<MemberAdminResponseDto>> getMemberList(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) @NonNull Pageable pageable) {

        Page<MemberAdminResponseDto> members = memberService.getMembersForAdmin(pageable);
        return ResponseEntity.ok(members);
    }

    // 회원 정지(Ban) 해제(Unban) 토글
    @PatchMapping("/members/{memberId}/ban")
    public ResponseEntity<Void> toggleBanStatus(
            @NonNull @PathVariable("memberId") Long memberId,
            @RequestParam("status") boolean shouldBan) {

        log.info("사용자 계정 수동 잠금:{},{}", memberId, shouldBan);
        memberService.updateBanStatus(memberId, shouldBan);
        return ResponseEntity.noContent().build(); // 244 No Content 반환
    }

    // 회원 계정 임시 잠금 수동 해제
    @PostMapping("/members/{memberId}/unlock")
    public ResponseEntity<Void> unlockMember(
            @NonNull @PathVariable("memberId") Long memberId) {

        memberService.releaseMemberLock(memberId);
        return ResponseEntity.noContent().build();
    }
}
