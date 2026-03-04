package com.finance.finportfolio.domain.finance.dto;

/*
     * 고든 성장 모델 계산
     * dps: 현재 배당금
     * growthRate: 배당 성장률
     * expectedYield: 기대수익률
     */
public record CalculateFairRequestDto(
        double dps,
        double expectedYield,
        double growthRate) {
}
