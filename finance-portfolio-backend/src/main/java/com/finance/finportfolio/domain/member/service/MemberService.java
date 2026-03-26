package com.finance.finportfolio.domain.member.service;

import com.finance.finportfolio.domain.member.dto.MemberJoinRequestDto;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequestDto;
import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.entity.RefreshToken;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.domain.member.repository.RefreshTokenRepository;
import com.finance.finportfolio.global.error.exception.DuplicateResourceException;
import com.finance.finportfolio.global.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

        // ── 로그인 → [accessToken, refreshToken] 반환 ─────────────
        @Transactional
        public String[] login(MemberLoginRequestDto request) {

                // 1. 아이디/비밀번호 인증
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.loginId(), request.password()));

                String loginId = authentication.getName();
                String role = authentication.getAuthorities().iterator().next().getAuthority();

                // 2. 토큰 생성
                String accessToken = jwtTokenProvider.createAccessToken(loginId, role);
                String refreshToken = jwtTokenProvider.createRefreshToken(loginId);

                // 3. Refresh Token DB 저장 (없으면 insert, 있으면 rotate)
                Member member = findMemberByLoginId(loginId);

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

                return new String[] { accessToken, refreshToken };
        }

        // ── 로그아웃 → Refresh Token DB에서 삭제 ──────────────────
        @Transactional
        public void logout(String loginId) {

                Member member = findMemberByLoginId(loginId);

                refreshTokenRepository.deleteByMember(member);
        }

        // ── 토큰 재발급 (Refresh Token Rotation) ──────────────────
        @Transactional
        public String[] reissue(String refreshToken) {

                // 1. Refresh Token 유효성 검증
                if (!jwtTokenProvider.validateToken(refreshToken)) {
                        throw new IllegalStateException("유효하지 않은 Refresh Token입니다.");
                }

                String loginId = jwtTokenProvider.getLoginId(refreshToken);

                // 2. DB에 저장된 토큰과 일치 여부 확인 (탈취 감지)

                Member member = findMemberByLoginId(loginId);

                RefreshToken savedToken = refreshTokenRepository.findByMember(member)
                                .orElseThrow(() -> new IllegalStateException("로그인 상태가 아닙니다."));

                if (!savedToken.getToken().equals(refreshToken)) {
                        // 저장된 토큰과 다르면 탈취 가능성 → 강제 로그아웃
                        refreshTokenRepository.deleteByMember(member);
                        throw new IllegalStateException("Refresh Token이 일치하지 않습니다.");
                }

                // 3. 새 토큰 발급 + Rotation
                String role = member.getRole().getKey();
                String newAccessToken = jwtTokenProvider.createAccessToken(loginId, role);
                String newRefreshToken = jwtTokenProvider.createRefreshToken(loginId);

                savedToken.rotate(newRefreshToken);

                return new String[] { newAccessToken, newRefreshToken };
        }
}