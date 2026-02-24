package com.finance.finportfolio;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
// JPA시간용
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;

@EnableJpaAuditing
@SpringBootApplication
@ConfigurationPropertiesScan("com.finance.finportfolio.infrastructure.file")
public class FinancePortfolioApplication {

    public static void main(String[] args) {
        String profile = System.getProperty("spring.profiles.active");

        // 로컬 환경일 때만 .env 로드 시도
        if (profile == null || profile.contains("local")) {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        }

        SpringApplication.run(FinancePortfolioApplication.class, args);
    }

    @PostConstruct
    public void started() {
        // 서버의 물리적 위치와 상관없이 기준시를 서울로 고정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }

}
