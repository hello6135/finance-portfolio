package com.finance.finportfolio.domain.member.service;

import com.finance.finportfolio.domain.member.dto.LoginResultDto;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberResponseDto;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.entity.RefreshToken;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.domain.member.repository.RefreshTokenRepository;
import com.finance.finportfolio.global.error.exception.DuplicateResourceException;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

        private final MemberRepository memberRepository;
        private final RefreshTokenRepository refreshTokenRepository;
        private final PasswordEncoder passwordEncoder;
        private final AuthenticationManager authenticationManager;
        private final JwtTokenProvider jwtTokenProvider;
        private final LoginAttemptService loginAttemptService;

        @Transactional(readOnly = true)
        public long getTotalMemberCount() {
                return memberRepository.count();
        }

        private Member findMemberByLoginId(String loginId) {
                return memberRepository.findByLoginId(loginId)
                                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));
        }

        // ── 회원가입 ───────────────────────────────────────────────
        @Transactional
        public Long join(MemberJoinRequestDto request) {
                memberRepository.findByLoginId(request.loginId())
                                .ifPresent(m -> {
                                        throw new DuplicateResourceException("이미 존재하는 아이디입니다.");
                                });

                String encodedPassword = passwordEncoder.encode(request.password());

                Member member = Member.builder()
                                .loginId(request.loginId())
                                .password(encodedPassword)
                                .nickname(request.nickname())
                                .role(Role.USER)
                                .build();

                return memberRepository.save(member).getId();
        }

        // ── 로그인 ─────────────
        // LoginResultDto: 서버 내부 DTO [accessToken, refreshToken, MemberResponseDto]
        // MemberResponseDto: 프론트 반환 DTO [nickname, role]
        @Transactional
        public LoginResultDto login(MemberLoginRequestDto request) {

                // 아이디 있는지부터 체크
                // 아이디 존재 여부를 숨기기 위해 findMemberByLoginId를 사용하지 않고 별도 예외처리
                Member member = memberRepository.findByLoginId(request.loginId())
                                .orElseThrow(() -> new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다."));

                // [무차별대입방어] 잠금 상태 체크
                member.checkLockStatus();

                try {
                        // 인증시도 - 비밀번호 체크
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        member.getLoginId(), request.password()));

                        // [무차별대입방어] 로그인 성공 초기화
                        member.loginSuccess();

                        // 고객 정보 get
                        String nickName = member.getNickname();
                        Role role = member.getRole();

                        // 토큰 생성
                        String accessToken = jwtTokenProvider.createAccessToken(member.getLoginId(), role.name());
                        String refreshToken = jwtTokenProvider.createRefreshToken(member.getLoginId());

                        refreshTokenRepository.findByMember(member)
                                        .ifPresentOrElse(
                                                        existing -> existing.rotate(refreshToken), // 재로그인: 토큰 교체
                                                        () -> { // 최초 로그인: 신규 저장
                                                                final RefreshToken newToken = RefreshToken.builder()
                                                                                .member(member)
                                                                                .token(refreshToken)
                                                                                .build();
                                                                refreshTokenRepository.save(newToken);
                                                        });

                        // 토큰 및 고객정보 반환
                        MemberResponseDto memberResponseDto = new MemberResponseDto(nickName, role);
                        return LoginResultDto.builder()
                                        .accessToken(accessToken)
                                        .refreshToken(refreshToken)
                                        .memberResponseDto(memberResponseDto)
                                        .build();
                } catch (BadCredentialsException e) {
                        // [무차별 대입 방어] 로그인 실패 잠금 카운트 증가
                        // Transactional이 예외처리 시 엔티티 변경사항도 초기화!
                        // 트랜잭션을 별도 서비스로 분리하여 초기화 방지
                        loginAttemptService.updateFailCount(member.getId());

                        throw new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다.");
                }
        }

        // ── 로그아웃 → Refresh Token DB에서 삭제 ──────────────────
        @Transactional
        public void logout(String loginId) {

                Member member = findMemberByLoginId(loginId);

                refreshTokenRepository.deleteByMember(member);
        }

        // ── 토큰 재발급 (Refresh Token Rotation) ──────────────────
        // LoginResultDto: 서버 내부 DTO [accessToken, refreshToken, MemberResponseDto]
        // MemberResponseDto: 프론트 반환 DTO [nickname, role]
        @Transactional
        public LoginResultDto reissue(String refreshToken) {

                // 1. Refresh Token 유효성 검증
                if (!jwtTokenProvider.validateToken(refreshToken)) {
                        throw new IllegalStateException("유효하지 않은 Refresh Token입니다.");
                }

                String loginId = jwtTokenProvider.getLoginId(refreshToken);

                // DB에서 loginId로 Member find
                Member member = findMemberByLoginId(loginId);

                // 토큰 생성
                RefreshToken savedToken = refreshTokenRepository.findByMember(member)
                                .orElseThrow(() -> new IllegalStateException("로그인 상태가 아닙니다."));

                if (!savedToken.getToken().equals(refreshToken)) {
                        // 저장된 토큰과 다르면 탈취 가능성 → 강제 로그아웃
                        refreshTokenRepository.deleteByMember(member);
                        throw new IllegalStateException("Refresh Token이 일치하지 않습니다.");
                }

                // 고객 정보 get
                String nickName = member.getNickname();
                Role role = member.getRole();

                // 새 토큰 발급
                String newAccessToken = jwtTokenProvider.createAccessToken(loginId, role.name());
                String newRefreshToken = jwtTokenProvider.createRefreshToken(loginId);
                // 더티 체킹 업데이트
                savedToken.rotate(newRefreshToken);

                // 토큰 및 고객정보 반환
                MemberResponseDto memberResponseDto = new MemberResponseDto(nickName, role);
                return LoginResultDto.builder()
                                .accessToken(newAccessToken)
                                .refreshToken(newRefreshToken)
                                .memberResponseDto(memberResponseDto)
                                .build();
        }
}