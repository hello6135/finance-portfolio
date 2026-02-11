package com.finance.finportfolio.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Configuration
@Profile("dev")
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Bean // 다른 클래스 의존성 주입용
    public AmazonS3 amazonS3() {
        // 1. 발급받은 키를 사용하여 자격 증명 객체 생성
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        // 2. S3 클라이언트 빌더를 통해 리전과 자격 증명을 설정하고 객체 생성
        return AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(credentials)) // 고정 키 방식인데 EC2 배포시 동적으로 변경!
                .build();
    }
}