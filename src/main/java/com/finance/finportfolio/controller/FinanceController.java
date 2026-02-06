package com.finance.finportfolio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finance.finportfolio.domain.Portfolio;
import com.finance.finportfolio.service.PortfolioService;

import lombok.RequiredArgsConstructor;

/**
 * FinanceController - 포트폴리오 관련 REST API를 제공하는 Controller
 * 
 * 🎯 REST API 방식 (JSON 반환)
 * - @RestController 사용
 * - @PostController와 달리 HTML이 아닌 JSON 데이터를 반환
 * - 프론트엔드나 모바일 앱에서 사용할 수 있는 API 제공
 * 
 * 📝 Service 레이어 적용:
 * - 포트폴리오 계산 로직은 PortfolioService로 이동
 * - Controller는 HTTP 요청/응답만 처리
 */
@RestController // JSON을 반환하는 REST API 컨트롤러
@RequiredArgsConstructor
public class FinanceController {

    // Repository 대신 Service를 주입받습니다
    // 비즈니스 로직(계산, 검증 등)은 Service에서 처리
    private final PortfolioService portfolioService;

    /**
     * 포트폴리오 저장 API
     * 
     * @param name  사용자 이름
     * @param stock 주식 비중 (0.0 ~ 1.0)
     * @return 저장 성공 메시지
     * 
     *         사용 예: /api/save?name=홍길동&stock=0.7
     */
    @GetMapping("/api/save")
    public String save(@RequestParam String name, @RequestParam double stock) {
        // Service를 통해 포트폴리오 저장
        // Service 내부에서 채권 비중 계산, 위험도 분류 등 모든 비즈니스 로직 처리
        portfolioService.savePortfolio(name, stock);
        return name + "님의 포트폴리오가 DB에 저장되었습니다.";
    }

    /**
     * 모든 포트폴리오 조회 API
     * 
     * @return 포트폴리오 목록 (JSON 형식)
     * 
     *         사용 예: /api/all
     */
    @GetMapping("/api/all")
    public List<Portfolio> getAll() {
        // Service를 통해 모든 포트폴리오 조회
        return portfolioService.getAllPortfolios();
    }

    /**
     * 평균 주식 비중 조회 API
     * 
     * @return 평균 주식 비중 메시지
     * 
     *         사용 예: /api/average
     * 
     *         💡 비즈니스 로직(통계 계산)은 Service에서 처리
     */
    @GetMapping("/api/average")
    public String getAverage() {
        // Service를 통해 평균 계산 및 메시지 생성
        return portfolioService.getAverageStockWeightMessage();
    }
}
