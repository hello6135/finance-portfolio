package com.finance.finportfolio.domain.finance.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity // 이 클래스대로 DB에 테이블을 만들라는 뜻
@Getter
@Setter
public class Portfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // 투자자 이름
    private double stockWeight; // 주식 비중
    private double bondWeight; // 채권 비중
    private String riskType; // 공격형/안정형 결과
}