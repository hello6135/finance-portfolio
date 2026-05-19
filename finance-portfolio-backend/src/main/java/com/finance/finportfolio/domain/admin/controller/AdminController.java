package com.finance.finportfolio.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.admin.dto.AdminResponseDto;
import com.finance.finportfolio.domain.admin.service.AdminService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/summary")
    public ResponseEntity<AdminResponseDto> getCounts() {
        AdminResponseDto summary = adminService.getDashboardSummary();
        return ResponseEntity.ok(summary);
    }
}
