package com.finance.finportfolio.domain.member.entity;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.authentication.LockedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // [무차별대입방어]
    @Column(nullable = false)
    private int loginFailCount = 0; // 로그인 실패 횟수
    private LocalDateTime lockedUntil; // 언제까지 잠글 것인가

    @Builder
    public Member(String loginId, String password, String nickname, Role role) {
        this.loginId = loginId;
        this.password = password;
        this.nickname = nickname;
        this.role = role != null ? role : Role.USER;
        this.loginFailCount = 0;
    }

    // [무차별대입방어]
    // 잠금 상태 체크(조회)
    public boolean isLocked() {
        return this.lockedUntil != null && this.lockedUntil.isAfter(LocalDateTime.now());
    }

    // 잠금 상태 체크(검증 및 예외처리)
    public void checkLockStatus() {
        if (isLocked()) {
            long remainingMinutes = Duration.between(LocalDateTime.now(), this.lockedUntil).toMinutes();
            StringBuilder message = new StringBuilder("인증에 5회 이상 실패했습니다. ");
            if (remainingMinutes > 0) {
                message.append(remainingMinutes).append("분 후에 다시 시도해주세요.");
            } else {
                // 1분 미만으로 남았을 때
                message.append("잠시 후 다시 시도해주세요.");
            }
            throw new LockedException(message.toString());
        }
    }

    public void loginFailed() {
        this.loginFailCount++;
        // 5회 실패 시 5분 잠금
        if (this.loginFailCount >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(5);
        }
    }

    public void loginSuccess() {
        // 성공시 초기화
        this.loginFailCount = 0;
        this.lockedUntil = null;
    }

}
