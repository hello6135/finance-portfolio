package com.finance.finportfolio.domain.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .loginId("testUser")
                .password("encoded_password")
                .nickname("테스터")
                .role(Role.USER)
                .build();
    }

    @Nested
    @DisplayName("updateFailCount 호출 시")
    class UpdateFailCount {

        @Test
        @DisplayName("실패 횟수가 1 증가한다")
        void increaseFailCountByOne() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when
            loginAttemptService.updateFailCount(1L);

            // then
            assertThat(member.getLoginFailCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("4회 실패 시 계정이 잠기지 않는다")
        void doesNotLockAfterFourFailures() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when: 4번 실패
            for (int i = 0; i < 4; i++) {
                loginAttemptService.updateFailCount(1L);
            }

            // then
            assertThat(member.getLoginFailCount()).isEqualTo(4);
            assertThat(member.isLocked()).isFalse();
        }

        @Test
        @DisplayName("5회 실패 시 계정이 5분간 잠긴다")
        void locksAccountAfterFiveFailures() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when: 5번 실패
            for (int i = 0; i < 5; i++) {
                loginAttemptService.updateFailCount(1L);
            }

            // then
            assertThat(member.getLoginFailCount()).isEqualTo(5);
            assertThat(member.isLocked()).isTrue();
        }

        @Test
        @DisplayName("5회 초과 실패해도 잠금 상태가 유지된다")
        void remainsLockedAfterMoreThanFiveFailures() {
            // given
            given(memberRepository.findById(1L)).willReturn(Optional.of(member));

            // when: 7번 실패
            for (int i = 0; i < 7; i++) {
                loginAttemptService.updateFailCount(1L);
            }

            // then
            assertThat(member.getLoginFailCount()).isEqualTo(7);
            assertThat(member.isLocked()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 memberId이면 예외가 발생한다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loginAttemptService.updateFailCount(999L))
                    .isInstanceOf(java.util.NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("Member 엔티티 잠금 상태 검증")
    class MemberLockBehavior {

        @Test
        @DisplayName("잠금 시간이 현재보다 미래이면 isLocked()는 true를 반환한다")
        void isLockedReturnsTrueWhenLockedUntilIsInFuture() {
            // given
            ReflectionTestUtils.setField(member, "lockedUntil", LocalDateTime.now().plusMinutes(5));

            // then
            assertThat(member.isLocked()).isTrue();
        }

        @Test
        @DisplayName("잠금 시간이 현재보다 과거이면 isLocked()는 false를 반환한다")
        void isLockedReturnsFalseWhenLockedUntilIsInPast() {
            // given
            ReflectionTestUtils.setField(member, "lockedUntil", LocalDateTime.now().minusMinutes(1));

            // then
            assertThat(member.isLocked()).isFalse();
        }

        @Test
        @DisplayName("lockedUntil이 null이면 isLocked()는 false를 반환한다")
        void isLockedReturnsFalseWhenLockedUntilIsNull() {
            // then
            assertThat(member.isLocked()).isFalse();
        }

        @Test
        @DisplayName("로그인 성공 시 failCount와 lockedUntil이 초기화된다")
        void loginSuccessResetsFailCountAndLockedUntil() {
            // given: 5회 실패 상태
            for (int i = 0; i < 5; i++) {
                member.loginFailed();
            }
            assertThat(member.isLocked()).isTrue();

            // when
            member.loginSuccess();

            // then
            assertThat(member.getLoginFailCount()).isEqualTo(0);
            assertThat(member.isLocked()).isFalse();
        }

        @Test
        @DisplayName("checkLockStatus()는 잠긴 상태일 때 LockedException을 던진다")
        void checkLockStatusThrowsLockedException() {
            // given
            ReflectionTestUtils.setField(member, "lockedUntil", LocalDateTime.now().plusMinutes(3));

            // when & then
            assertThatThrownBy(member::checkLockStatus)
                    .isInstanceOf(org.springframework.security.authentication.LockedException.class)
                    .hasMessageContaining("5회 이상 실패");
        }

        @Test
        @DisplayName("checkLockStatus()는 잠금 해제 상태일 때 예외를 던지지 않는다")
        void checkLockStatusDoesNotThrowWhenNotLocked() {
            // given: 잠금 없음
            // when & then
            assertThatNoException().isThrownBy(member::checkLockStatus);
        }
    }
}