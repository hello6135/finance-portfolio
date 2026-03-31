package com.finance.finportfolio.global.error;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.finance.finportfolio.global.error.exception.DuplicateResourceException;
import com.finance.finportfolio.global.error.exception.RefreshTokenNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice // 모든 컨트롤러의 예외를 여기서 캐치
public class GlobalExceptionHandler {

    // -- 공통 메서드 --

    // 공통 응답 생성 메서드
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String code, String message) {
        ErrorResponse response = ErrorResponse.builder()
                .status(status.value())
                .code(code)
                .message(message)
                .build();
        return new ResponseEntity<>(response, status);
    }

    // 메시지 빈 값 대비
    // e.getMessage()로 에러 메시지 전달 받았으면 exceptionMessage, 아니면 defaultMessage 반환
    private String resolveMessage(String exceptionMessage, String defaultMessage) {
        return Optional.ofNullable(exceptionMessage)
                .filter(msg -> !msg.isBlank())
                .orElse(defaultMessage);
    }

    // -- 예외 핸들러 --

    // 400 BAD_REQUEST
    // INVALID_INPUT: 잘못된 인자
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        String message = resolveMessage(e.getMessage(), "🛠입력 데이터가 올바르지 않습니다.");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "INVALID_INPUT", message);
    }

    // 400 BAD_REQUEST
    // ILLEGAL_STATE: 잘못된 상태
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(IllegalStateException e) {
        String message = resolveMessage(e.getMessage(), "🛠현재 요청을 처리할 수 없는 상태입니다.");
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "ILLEGAL_STATE", message);
    }

    // 400 BAD_REQUEST
    // VALIDATION_ERROR: @Valid 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        // @Valid 조건에 적어놓은 message 가져옴(명시하지 않은 경우 Hibernate Validator에 내장된 기본 메시지)
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("[%s] %s", error.getField(), error.getDefaultMessage()))
                .findFirst()
                .orElse("🛠입력값이 올바르지 않습니다.");

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    // 401 UNAUTHORIZED
    // 401: 인증 실패 통합 관리(아이디 조회 실패, 비밀번호 오입력 등 각종 로그인 실패)
    @ExceptionHandler({
            UsernameNotFoundException.class,
            BadCredentialsException.class,
            AuthenticationException.class })
    public ResponseEntity<ErrorResponse> handleAuthException(Exception e) {
        String defaultMsg = "🛠인증에 실패하였습니다.";
        String code = "UNAUTHORIZED";

        // 1순위: 가장 구체적인 '사용자 없음'
        if (e instanceof UsernameNotFoundException) {
            defaultMsg = "🛠존재하지 않는 사용자입니다.";
            code = "USER_NOT_FOUND";
        }
        // 2순위: '비밀번호 틀림'
        else if (e instanceof BadCredentialsException) {
            defaultMsg = "🛠아이디 또는 비밀번호가 일치하지 않습니다.";
            code = "BAD_CREDENTIALS";
        }
        // 3순위: 그 외 기타 인증 에러 (토큰 만료, 접근 거부 등)
        // 여기서는 기본 설정된 defaultMsg와 code 반환.

        String message = resolveMessage(e.getMessage(), defaultMsg);
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, code, message);
    }

    // 401 UNAUTHORIZED
    // REFRESH_TOKEN_NOT_FOUND: 리프레쉬 토큰 없음
    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRefreshTokenNotFoundException(RefreshTokenNotFoundException e) {
        String message = resolveMessage(e.getMessage(), "🛠Refresh Token이 없습니다.");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", message);
    }

    // 409 CONFLICT
    // DUPLICATE_RESOURCE: 중복 아이디 커스텀 예외
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateException(DuplicateResourceException e) {
        String message = resolveMessage(e.getMessage(), "🛠이미 존재하는 리소스입니다.");
        return buildErrorResponse(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }

    // 500 INTERNAL_SERVER_ERROR
    // INTERNAL_SERVER_ERROR: 그 외 예상치 못한 모든 에러(500) 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllException(Exception e) {
        log.error("서버 내부 에러 발생!", e);
        String message = resolveMessage(e.getMessage(), "예상치 못한 문제가 발생했습니다. 이용에 불편을 드려 죄송합니다.");
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", message);
    }
}