package com.finance.finportfolio.domain.member.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.entity.RefreshToken;
import com.finance.finportfolio.domain.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private Member createMember() {
        return Member.builder()
                .loginId("testUser")
                .password("encodedPassword")
                .nickname("tester")
                .role(Role.USER)
                .build();
    }

    @Test
    @DisplayName("RefreshToken 생성 시 member와 token이 정상적으로 저장된다")
    void create_Success() {
        Member member = createMember();

        RefreshToken refreshToken = RefreshToken.builder()
                .member(member)
                .token("originalToken")
                .build();

        assertThat(refreshToken.getMember()).isEqualTo(member);
        assertThat(refreshToken.getToken()).isEqualTo("originalToken");
    }

    @Test
    @DisplayName("rotate() 호출 시 토큰이 새 값으로 교체된다")
    void rotate_Success() {
        Member member = createMember();
        RefreshToken refreshToken = RefreshToken.builder()
                .member(member)
                .token("originalToken")
                .build();

        refreshToken.rotate("newToken");

        assertThat(refreshToken.getToken()).isEqualTo("newToken");
    }

    @Test
    @DisplayName("rotate()를 여러 번 호출해도 항상 최신 토큰으로 교체된다")
    void rotate_Multiple_Success() {
        Member member = createMember();
        RefreshToken refreshToken = RefreshToken.builder()
                .member(member)
                .token("token1")
                .build();

        refreshToken.rotate("token2");
        refreshToken.rotate("token3");

        // 최종적으로 마지막 토큰만 남아야 함
        assertThat(refreshToken.getToken()).isEqualTo("token3");
    }

    @Test
    @DisplayName("id는 기본적으로 null이고 ReflectionTestUtils로 설정 가능하다")
    void id_DefaultNull() {
        Member member = createMember();
        RefreshToken refreshToken = RefreshToken.builder()
                .member(member)
                .token("token")
                .build();

        assertThat(refreshToken.getId()).isNull();

        ReflectionTestUtils.setField(refreshToken, "id", 1L);
        assertThat(refreshToken.getId()).isEqualTo(1L);
    }
}