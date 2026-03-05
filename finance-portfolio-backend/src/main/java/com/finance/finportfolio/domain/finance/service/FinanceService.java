package com.finance.finportfolio.domain.finance.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FinanceService {

    /*
     * 고든 성장 모델 계산
     * dps: 현재 배당금
     * growthRate: 배당 성장률
     * expectedYield: 기대수익률
     */
    public double calculateFairValue(double dps, double expectedYield, double growthRate) {
        if (expectedYield <= growthRate) {
            throw new IllegalArgumentException("기대수익률은 성장률보다 커야 합니다.");
        }

        // 2. 고든 성장 모델: V = (D1) / (k - g)
        // 여기서 D1 = D0 * (1 + g)
        double kDecimal = expectedYield / 100;
        double gDecimal = growthRate / 100;

        return (dps * (1 + gDecimal)) / (kDecimal - gDecimal);
    }
}
