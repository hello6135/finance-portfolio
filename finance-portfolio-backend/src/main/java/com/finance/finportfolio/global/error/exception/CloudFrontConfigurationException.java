package com.finance.finportfolio.global.error.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CloudFrontConfigurationException extends RuntimeException {
    public CloudFrontConfigurationException(String message) {
        super(message);
    }

    public CloudFrontConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}