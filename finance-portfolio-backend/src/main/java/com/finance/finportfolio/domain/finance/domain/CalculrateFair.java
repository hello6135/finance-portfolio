package com.finance.finportfolio.domain.finance.domain;

import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
     * 고든 성장 모델 계산
     * dps: 현재 배당금
     * growthRate: 배당 성장률
     * expectedYield: 기대수익률
     */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CalculrateFair {
    private double dps;
    private double expectedYield;
    private double growthRate;
}
