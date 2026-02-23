package com.finance.finportfolio.infrastructure.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.NonNull;

@ConfigurationProperties
public record S3Properties(
        @NonNull String bucketName,
        @NonNull String cloudfrontDomain) {

}
