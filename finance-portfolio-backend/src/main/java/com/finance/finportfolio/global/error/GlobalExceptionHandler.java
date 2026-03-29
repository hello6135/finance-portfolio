package com.finance.finportfolio.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.finance.finportfolio.global.error.exception.DuplicateResourceException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러의 예외를 여기서 캐치
public class GlobalExceptionHandler {

    // 인자 예외
    // 400 BAD_REQUEST
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 인자 유입: {}", e.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .code("BAD_REQUEST")
                .message(e.getMessage())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 상태 예외
    // 400 badRequest
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {

        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // @Valid 검증 실패
    // 400 badRequest
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("입력값이 올바르지 않습니다.");

        return ResponseEntity.badRequest().body(message);
    }

    // 중복 아이디 커스텀 예외
    // 409 Conflict
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<String> handleDuplicateException(DuplicateResourceException e) {

        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    // 아이디 못찾음 예외
    // 401 Unauthorized
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleUsernameNotFoundException(UsernameNotFoundException e) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }

    // 1. 로그인 실패 (사용자 입력 오류)
    // 401 Unauthorized
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 일치하지 않습니다.");
    }

    // 2. 그 외 모든 인증 실패 (토큰 문제 등)
    // 401 Unauthorized
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthException(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증 세션이 만료되었거나 유효하지 않습니다.");
    }

    // 그 외 예상치 못한 모든 에러(500) 처리
    // 500 INTERNAL_SERVER_ERROR
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e) {
        log.error("서버 내부 에러 발생!", e); // 스택 트레이스 전체 로그 기록

        ErrorResponse response = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_SERVER_ERROR")
                .message("서버 이용에 불편을 드려 죄송합니다. 관리자에게 문의하세요.")
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}