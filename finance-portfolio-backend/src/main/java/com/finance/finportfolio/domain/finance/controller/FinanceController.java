package com.finance.finportfolio.domain.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.finance.dto.CalculateFairRequestDto;
import com.finance.finportfolio.domain.finance.dto.CalculateFairResponseDto;
import com.finance.finportfolio.domain.finance.service.FinanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/finance")
public class FinanceController {

        private final FinanceService financeService;

        /*
         * 고든 성장 모델 계산
         * dps: 현재 배당금
         * growthRate: 배당 성장률
         * expectedYield: 기대수익률
         */
        @PostMapping("/fair") // POST 방식으로 변경
        public ResponseEntity<CalculateFairResponseDto> calculateFairValue(
                        @RequestBody CalculateFairRequestDto requestDto) {
                log.info("적정주가 계산 요청 - DPS: {}, Yield: {}, Growth: {}",
                                requestDto.dps(), requestDto.expectedYield(), requestDto.growthRate());

                // 비즈니스 로직 수행
                double value = financeService.calculateFairValue(
                                requestDto.dps(),
                                requestDto.expectedYield(),
                                requestDto.growthRate());

                // 응답 객체 생성
                CalculateFairResponseDto responseDto = new CalculateFairResponseDto(value, "계산이 완료되었습니다.");

                return ResponseEntity.ok(responseDto);
        }
}
