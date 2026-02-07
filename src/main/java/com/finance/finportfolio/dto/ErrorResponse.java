package com.finance.finportfolio.dto;

import lombok.Builder;
import lombok.Getter;

// ErrorResponse - 예외 처리용 DTO
@Getter
@Builder
public class ErrorResponse {
    private int status;
    private String code;
    private String message;

}
