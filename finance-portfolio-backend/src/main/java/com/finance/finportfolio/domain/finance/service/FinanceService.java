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
        if (growthRate > 0) {
            return dps * (1 + growthRate) / (expectedYield - growthRate);
        }
        return dps / expectedYield;
    }
}
