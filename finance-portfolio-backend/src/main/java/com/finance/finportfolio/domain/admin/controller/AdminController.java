package com.finance.finportfolio.domain.admin.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.admin.service.AdminService;
import com.finance.finportfolio.domain.admin.service.AwsHealthCheckService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
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
}
