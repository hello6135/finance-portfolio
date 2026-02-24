package com.finance.finportfolio.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local | dev | prod")
public class S3Config {
    // AWS 인증 정보 처리 - Access key, Secret Key
    // S3Client 빈 생성 - 실제 파일 업로드/삭제
    // 보안 및 권한 부여 방식 결정
}