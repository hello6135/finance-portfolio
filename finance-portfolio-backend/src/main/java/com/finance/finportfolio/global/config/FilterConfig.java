package com.finance.finportfolio.global.config;

import com.finance.finportfolio.global.filter.CloudFrontHeaderFilter;
import org.springframework.beans.factory.annotation.Value; // import 추가
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FilterConfig {

    @Value("${cloudfront.custom.header.name}")
    private String cfHeaderName;

    @Value("${cloudfront.custom.header.value}")
    private String cfHeaderValue;

    @Bean
    @Profile({ "dev", "prod" })
    public FilterRegistrationBean<CloudFrontHeaderFilter> cloudFrontHeaderFilterRegistration() {
        // 필터를 여기서 직접 생성 (빈 주입을 기다리지 않음)
        try {
            CloudFrontHeaderFilter filter = new CloudFrontHeaderFilter(cfHeaderName, cfHeaderValue);

            FilterRegistrationBean<CloudFrontHeaderFilter> registrationBean = new FilterRegistrationBean<>();
            registrationBean.setFilter(filter);
            registrationBean.addUrlPatterns("/api/*");
            registrationBean.setOrder(1);

            return registrationBean;
        } catch (Exception e) {
            throw new RuntimeException("CloudFront Header Filter 초기화 실패: " + e.getMessage());
        }

    }
}