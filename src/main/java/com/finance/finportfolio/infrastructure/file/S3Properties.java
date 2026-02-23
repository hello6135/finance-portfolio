package com.finance.finportfolio.infrastructure.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.NonNull;

@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
public record S3Properties(
        @NonNull String bucketName,
        @NonNull String cloudfrontDomain) {

}
