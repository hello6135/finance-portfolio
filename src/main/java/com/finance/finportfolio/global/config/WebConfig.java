package com.finance.finportfolio.global.config;

import java.io.File;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.filter.HiddenHttpMethodFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 이미지 업로드 경로 주소 환경변수로 받아옴
    @Value("${file.upload-dir:upload_images}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 실제 파일이 저장된 물리적 경로
        String uploadPath = Paths.get(System.getProperty("user.dir"), uploadDir).toString();

        // 경로 끝에 슬래시(/)가 빠지면 인식이 안 될 수 있으므로 보정
        if (!uploadPath.endsWith(File.separator)) {
            uploadPath += File.separator;
        }

        // 브라우저에서 uploadDir 로 접근하면 위 경로의 파일을 찾아줌
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:///" + uploadPath + "/");

        log.info("이미지 리소스 매핑 완료: /images/** -> {}", uploadPath);
    }

    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }
}
