package com.finance.finportfolio.domain.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.finance.dto.CalculateFairRequestDto;
import com.finance.finportfolio.domain.finance.service.FinanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fin")
public class FinanceController {

    private final FinanceService financeService;

    /*
     * 고든 성장 모델 계산
     * dps: 현재 배당금
     * growthRate: 배당 성장률
     * expectedYield: 기대수익률
     */
    @GetMapping("/fair")
    public ResponseEntity<Double> calculateFairValue(@RequestBody CalculateFairRequestDto requestDto) {
        log.info("DPS: {}, ExpectedYield: {}, GrowthLate: {}", requestDto.dps(), requestDto.expectedYield(),
                requestDto.growthRate());

        double value = financeService.calculateFairValue(requestDto.dps(), requestDto.expectedYield(),
                requestDto.growthRate());
        return ResponseEntity.ok(value);

    }
}
