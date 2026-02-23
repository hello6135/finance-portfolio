package com.finance.finportfolio.infrastructure.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.NonNull;

@ConfigurationProperties(prefix = "cloud.aws.s3")
public record S3Properties(
        @NonNull String bucketName,
        @NonNull String cloudfrontDomain) {

}
