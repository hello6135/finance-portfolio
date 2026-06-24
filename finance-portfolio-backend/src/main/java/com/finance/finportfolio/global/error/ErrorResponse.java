package com.finance.finportfolio.global.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ErrorResponse - 예외 처리용 DTO
@Getter
@Builder
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 모든 필드 생성자 추가
public class ErrorResponse {
    private int status;
    private String code;
    private String message;

}
