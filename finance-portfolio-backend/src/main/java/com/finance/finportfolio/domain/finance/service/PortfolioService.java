package com.finance.finportfolio.domain.finance.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.finance.domain.Portfolio;
import com.finance.finportfolio.domain.finance.domain.PortfolioRepository;

import lombok.RequiredArgsConstructor;

/**
 * PortfolioService - 포트폴리오 비즈니스 로직을 담당하는 Service 레이어
 * 
 * 📊 포트폴리오 관련 계산과 검증 로직을 여기서 처리합니다
 * - 주식/채권 비중 계산
 * - 위험도 분류
 * - 통계 계산 (평균 등)
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    /**
     * 포트폴리오 저장 및 자동 계산
     * 
     * @param name        사용자 이름
     * @param stockWeight 주식 비중 (0.0 ~ 1.0)
     * @return 저장된 포트폴리오 객체
     * 
     *         🎯 비즈니스 로직:
     *         1. 주식 비중을 받으면 자동으로 채권 비중 계산 (1.0 - stockWeight)
     *         2. 주식 비중에 따라 위험도 자동 분류 (0.7 이상이면 공격형)
     *         3. 데이터 검증 (비중이 0~1 사이인지 확인 - 나중에 추가 가능)
     */
    public Portfolio savePortfolio(String name, double stockWeight) {
        // 1. Portfolio 객체 생성
        Portfolio portfolio = new Portfolio();
        portfolio.setName(name);
        portfolio.setStockWeight(stockWeight);

        // 2. 채권 비중 자동 계산 (총합이 1.0이 되도록)
        double bondWeight = 1.0 - stockWeight;
        portfolio.setBondWeight(bondWeight);

        // 3. 위험도 자동 분류 (주식 비중 70% 이상이면 공격형)
        String riskType = stockWeight >= 0.7 ? "공격형" : "안정형";
        portfolio.setRiskType(riskType);

        // 4. DB에 저장
        return portfolioRepository.save(portfolio);
    }

    /**
     * 모든 포트폴리오 조회
     * 
     * @return 포트폴리오 목록
     */
    public List<Portfolio> getAllPortfolios() {
        return portfolioRepository.findAll();
    }

    /**
     * 전체 포트폴리오의 평균 주식 비중 계산
     * 
     * @return 평균 주식 비중 (0.0 ~ 1.0)
     * 
     *         💡 비즈니스 로직: 통계 계산을 Service에서 처리
     */
    public double calculateAverageStockWeight() {
        List<Portfolio> allPortfolios = portfolioRepository.findAll();

        // 데이터가 없으면 0 반환
        if (allPortfolios.isEmpty()) {
            return 0.0;
        }

        // 스트림을 사용하여 모든 주식 비중의 합을 구한 후 평균 계산
        double sum = allPortfolios.stream()
                .mapToDouble(Portfolio::getStockWeight)
                .sum();

        return sum / allPortfolios.size();
    }

    /**
     * 평균 주식 비중을 포맷된 문자열로 반환
     * 
     * @return "N명의 평균 주식 비중은 X%입니다" 형식의 문자열
     */
    public String getAverageStockWeightMessage() {
        List<Portfolio> allPortfolios = portfolioRepository.findAll();

        if (allPortfolios.isEmpty()) {
            return "데이터가 없습니다.";
        }

        double average = calculateAverageStockWeight();
        int count = allPortfolios.size();

        // 퍼센트로 변환하여 문자열 포맷팅 (예: 0.65 → 65.00%)
        return String.format("현재 등록된 %d명의 평균 주식 비중은 %.2f%%입니다.",
                count, average * 100);
    }
}
