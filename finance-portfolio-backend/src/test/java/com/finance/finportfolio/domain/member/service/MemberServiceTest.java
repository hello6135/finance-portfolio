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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

        @InjectMocks
        private MemberService memberService;

        @Mock
        private MemberRepository memberRepository;

        @Mock
        private RefreshTokenRepository refreshTokenRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private AuthenticationManager authenticationManager;

        @Mock
        private JwtTokenProvider jwtTokenProvider;

        // 테스트용 Member 생성 헬퍼
        private Member createMember(String loginId) {
                Member member = Member.builder()
                                .loginId(loginId)
                                .password("encodedPassword")
                                .nickname("tester")
                                .role(Role.USER)
                                .build();
                ReflectionTestUtils.setField(member, "id", 1L);
                return member;
        }

        // ── join ───────────────────────────────────────────────────

        @Test
        @DisplayName("회원가입 성공 - 비밀번호가 암호화되어 저장된다")
        void join_Success() {
                MemberJoinRequestDto request = new MemberJoinRequestDto("testId", "rawPassword", "tester");
                Member savedMember = createMember("testId");

                given(memberRepository.findByLoginId(request.loginId())).willReturn(Optional.empty());
                given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
                given(memberRepository.save(any(Member.class))).willReturn(savedMember);

                Long savedId = memberService.join(request);

                assertThat(savedId).isEqualTo(1L);
                verify(passwordEncoder, times(1)).encode("rawPassword");
                verify(memberRepository, times(1)).save(any(Member.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 중복 아이디면 예외가 발생하고 저장이 호출되지 않는다")
        void join_Fail_DuplicateId() {
                MemberJoinRequestDto request = new MemberJoinRequestDto("duplicateId", "password", "tester");
                given(memberRepository.findByLoginId("duplicateId"))
                                .willReturn(Optional.of(createMember("duplicateId")));

                assertThatThrownBy(() -> memberService.join(request))
                                .isInstanceOf(DuplicateResourceException.class)
                                .hasMessageContaining("이미 존재하는 아이디입니다.");

                verify(memberRepository, never()).save(any(Member.class));
        }

        // ── login ──────────────────────────────────────────────────

        @Test
        @DisplayName("로그인 성공 - Access Token과 Refresh Token이 반환된다")
        void login_Success() {
                MemberLoginRequestDto request = new MemberLoginRequestDto("testId", "rawPassword");
                Member member = createMember("testId");

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "testId", null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                given(authenticationManager.authenticate(any())).willReturn(authentication);
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("accessToken");
                given(jwtTokenProvider.createRefreshToken(any())).willReturn("refreshToken");
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

                String[] tokens = memberService.login(request);

                assertThat(tokens[0]).isEqualTo("accessToken");
                assertThat(tokens[1]).isEqualTo("refreshToken");
                // 최초 로그인 → save 호출 확인
                verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("재로그인 성공 - 기존 Refresh Token이 rotate()로 교체된다")
        void login_Success_Relogin_RotatesToken() {
                MemberLoginRequestDto request = new MemberLoginRequestDto("testId", "rawPassword");
                Member member = createMember("testId");
                RefreshToken existingToken = RefreshToken.builder()
                                .member(member)
                                .token("oldRefreshToken")
                                .build();

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "testId", null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                given(authenticationManager.authenticate(any())).willReturn(authentication);
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("newAccessToken");
                given(jwtTokenProvider.createRefreshToken(any())).willReturn("newRefreshToken");
                // 기존 토큰 존재 → rotate() 경로
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(existingToken));

                memberService.login(request);

                // save가 아닌 rotate()로 교체됐는지 확인
                assertThat(existingToken.getToken()).isEqualTo("newRefreshToken");
                verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("로그인 실패 - 잘못된 비밀번호면 예외가 발생한다")
        void login_Fail_WrongPassword() {
                MemberLoginRequestDto request = new MemberLoginRequestDto("testId", "wrongPassword");
                given(authenticationManager.authenticate(any()))
                                .willThrow(new BadCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

                assertThatThrownBy(() -> memberService.login(request))
                                .isInstanceOf(BadCredentialsException.class);

                verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        }

        // ── logout ─────────────────────────────────────────────────

        @Test
        @DisplayName("로그아웃 성공 - Refresh Token이 DB에서 삭제된다")
        void logout_Success() {
                Member member = createMember("testId");
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));

                memberService.logout("testId");

                verify(refreshTokenRepository, times(1)).deleteByMember(member);
        }

        @Test
        @DisplayName("로그아웃 실패 - 존재하지 않는 회원이면 예외가 발생한다")
        void logout_Fail_MemberNotFound() {
                given(memberRepository.findByLoginId("unknown")).willReturn(Optional.empty());

                assertThatThrownBy(() -> memberService.logout("unknown"))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("존재하지 않는 회원입니다.");

                verify(refreshTokenRepository, never()).deleteByMember(any());
        }

        // ── reissue ────────────────────────────────────────────────

        @Test
        @DisplayName("토큰 재발급 성공 - 새 토큰 2개가 반환되고 DB 토큰이 교체된다")
        void reissue_Success() {
                Member member = createMember("testId");
                RefreshToken savedToken = RefreshToken.builder()
                                .member(member)
                                .token("validRefreshToken")
                                .build();

                given(jwtTokenProvider.validateToken("validRefreshToken")).willReturn(true);
                given(jwtTokenProvider.getLoginId("validRefreshToken")).willReturn("testId");
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(savedToken));
                given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("newAccessToken");
                given(jwtTokenProvider.createRefreshToken(any())).willReturn("newRefreshToken");

                String[] tokens = memberService.reissue("validRefreshToken");

                assertThat(tokens[0]).isEqualTo("newAccessToken");
                assertThat(tokens[1]).isEqualTo("newRefreshToken");
                // DB 토큰이 rotate()로 교체됐는지 확인
                assertThat(savedToken.getToken()).isEqualTo("newRefreshToken");
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 유효하지 않은 토큰이면 예외가 발생한다")
        void reissue_Fail_InvalidToken() {
                given(jwtTokenProvider.validateToken("invalidToken")).willReturn(false);

                assertThatThrownBy(() -> memberService.reissue("invalidToken"))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("유효하지 않은 Refresh Token입니다.");
        }

        @Test
        @DisplayName("토큰 재발급 실패 - DB 토큰과 불일치 시 강제 로그아웃된다 (탈취 감지)")
        void reissue_Fail_TokenMismatch_ForcesLogout() {
                Member member = createMember("testId");
                RefreshToken savedToken = RefreshToken.builder()
                                .member(member)
                                .token("originalToken")
                                .build();

                given(jwtTokenProvider.validateToken("stolenToken")).willReturn(true);
                given(jwtTokenProvider.getLoginId("stolenToken")).willReturn("testId");
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(savedToken));

                assertThatThrownBy(() -> memberService.reissue("stolenToken"))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Refresh Token이 일치하지 않습니다.");

                // 탈취 감지 → 강제 로그아웃 (DB 토큰 삭제) 확인
                verify(refreshTokenRepository, times(1)).deleteByMember(member);
        }

        @Test
        @DisplayName("토큰 재발급 실패 - 로그인 상태가 아니면 예외가 발생한다")
        void reissue_Fail_NotLoggedIn() {
                Member member = createMember("testId");

                given(jwtTokenProvider.validateToken("refreshToken")).willReturn(true);
                given(jwtTokenProvider.getLoginId("refreshToken")).willReturn("testId");
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                // DB에 토큰 없음 → 로그인 상태 아님
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

                assertThatThrownBy(() -> memberService.reissue("refreshToken"))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("로그인 상태가 아닙니다.");
        }
}