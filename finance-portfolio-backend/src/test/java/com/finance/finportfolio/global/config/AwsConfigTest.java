package com.finance.finportfolio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;

class AwsConfigTest {

    // 스프링 컨텍스트의 생성, 빈 등록 및 프로퍼티 주입을 고속으로 테스트할 수 있는 유틸리티
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AwsConfig.class);

    @Test
    @DisplayName("정상적인 프로퍼티가 제공되면 AWS 관련 Bean들이 정상적으로 등록된다")
    void awsBeans_ShouldBeCreated_WhenPropertiesPresent() {
        contextRunner
                // 가상의 application.properties 환경변수 주입
                .withPropertyValues(
                        "spring.cloud.aws.region.static=ap-northeast-2",
                        "spring.cloud.aws.credentials.access-key=test-access-key",
                        "spring.cloud.aws.credentials.secret-key=test-secret-key")
                .run(context -> {
                    // StaticCredentialsProvider Bean 검증
                    assertThat(context).hasSingleBean(StaticCredentialsProvider.class);
                    StaticCredentialsProvider credentialsProvider = context.getBean(StaticCredentialsProvider.class);
                    assertThat(credentialsProvider.resolveCredentials().accessKeyId()).isEqualTo("test-access-key");
                    assertThat(credentialsProvider.resolveCredentials().secretAccessKey()).isEqualTo("test-secret-key");

                    // S3Client Bean 검증
                    assertThat(context).hasSingleBean(S3Client.class);
                    S3Client s3Client = context.getBean(S3Client.class);
                    assertThat(s3Client.serviceClientConfiguration().region()).isEqualTo(Region.AP_NORTHEAST_2);

                    // Ec2Client Bean 검증
                    assertThat(context).hasSingleBean(Ec2Client.class);
                    Ec2Client ec2Client = context.getBean(Ec2Client.class);
                    assertThat(ec2Client.serviceClientConfiguration().region()).isEqualTo(Region.AP_NORTHEAST_2);
                });
    }
}