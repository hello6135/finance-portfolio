package com.finance.finportfolio;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; //JPA시간용

import jakarta.annotation.PostConstruct;

@EnableJpaAuditing
@SpringBootApplication
public class PortfolioServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PortfolioServerApplication.class, args);
    }

    @PostConstruct
    public void started() {
        // 서버의 물리적 위치와 상관없이 앱 내 기준시를 서울로 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

}
