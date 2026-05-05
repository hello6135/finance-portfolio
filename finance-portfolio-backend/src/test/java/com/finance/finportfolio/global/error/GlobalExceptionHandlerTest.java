package com.finance.finportfolio.global.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.finance.finportfolio.global.config.SecurityConfig;
import com.finance.finportfolio.global.security.filter.IpRateLimitFilter;
import com.finance.finportfolio.global.security.filter.JwtAuthenticationFilter;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(controllers = TestExceptionController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private IpRateLimitFilter ipRateLimitFilter;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("IllegalArgumentException 발생 시 400 에러와 커스텀 메시지를 반환한다")
    void handleIllegalArgumentExceptionTest() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("특정 값이 잘못되었습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("메시지 없이 IllegalStateException 발생 시 🛠 기본 메시지를 반환한다")
    void handleIllegalStateExceptionDefaultTest() throws Exception {
        mockMvc.perform(get("/test/illegal-state-default"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("ILLEGAL_STATE"))
                .andExpect(jsonPath("$.message").value("🛠현재 요청을 처리할 수 없는 상태입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("UsernameNotFoundException 발생 시 401 에러와 전용 코드를 반환한다")
    void handleUsernameNotFoundExceptionTest() throws Exception {
        mockMvc.perform(get("/test/user-not-found"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("🛠존재하지 않는 사용자입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("BadCredentialsException 발생 시 401 에러와 전용 코드를 반환한다")
    void handleBadCredentialsTest() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("🛠아이디 또는 비밀번호가 일치하지 않습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("BadCredentialsException 발생 시 401 에러와 전용 코드를 반환한다")
    void RefreshTokenNotFoundExceptionTest() throws Exception {
        mockMvc.perform(get("/test/refresh-token-not-found"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("🛠Refresh Token이 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("BadCredentialsException 발생 시 401 에러와 전용 코드를 반환한다")
    void ExpiredJwtExceptionTest() throws Exception {
        mockMvc.perform(get("/test/expired-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("EXPIRED_TOKEN"))
                .andExpect(jsonPath("$.message").value("🛠토큰이 만료되었습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("LockedException 발생 시 429 에러와 전용 코드를 반환한다")
    void LockedExceptionTest() throws Exception {
        mockMvc.perform(get("/test/account-locked"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.message").value("🛠계정이 잠겼습니다. 잠시 후 다시 시도해주세요."))
                .andDo(print());
    }

    @Test
    @DisplayName("DuplicateResourceException 발생 시 409 에러를 반환한다")
    void handleDuplicateExceptionTest() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("CloudFront 설정 에러 발생 시 500 에러를 반환한다")
    void handleCloudFrontConfigurationExceptionTest() throws Exception {
        mockMvc.perform(get("/test/cloudfront-config-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("CLOUDFRONT_CONFIG_ERROR"))
                .andExpect(jsonPath("$.message").value("시스템 보안 설정에 문제가 발생했습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("예상치 못한 RuntimeException 발생 시 500 에러를 반환한다")
    void handleAllExceptionTest() throws Exception {
        mockMvc.perform(get("/test/runtime"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("DB 연결 오류"))
                .andDo(print());
    }
}