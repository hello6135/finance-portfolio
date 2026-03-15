package com.finance.finportfolio.domain.member.service;

import com.finance.finportfolio.domain.member.domain.Member;
import com.finance.finportfolio.domain.member.domain.RefreshToken;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;
import com.finance.finportfolio.domain.member.dto.MemberLoginRequest;
import com.finance.finportfolio.domain.member.domain.MemberRepository;
import com.finance.finportfolio.domain.member.domain.RefreshTokenRepository;
import com.finance.finportfolio.domain.member.domain.Role;
import com.finance.finportfolio.global.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
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
    private RefreshTokenRepository refreshTokenRepository; // ← 추가

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager; // ← 추가

    @Mock
    private JwtTokenProvider jwtTokenProvider; // ← 추가

    // ── join 테스트 (기존 유지) ────────────────────────────────
    @Test
    @DisplayName("회원가입 성공 - 비밀번호가 암호화되어 저장되어야 한다")
    void join_Success() {
        MemberJoinRequest request = new MemberJoinRequest("testId", "rawPassword", "tester");
        String encodedPassword = "encodedPassword123";

        given(memberRepository.findByLoginId(request.loginId())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);

        Member savedMember = Member.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .nickname(request.nickname())
                .build();
        ReflectionTestUtils.setField(savedMember, "id", 1L);

        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        Long savedId = memberService.join(request);

        assertThat(savedId).isEqualTo(1L);
        verify(passwordEncoder, times(1)).encode("rawPassword");
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 아이디인 경우 예외 발생")
    void join_Fail_DuplicateId() {
        MemberJoinRequest request = new MemberJoinRequest("duplicateId", "password", "tester");
        Member existingMember = Member.builder().loginId("duplicateId").build();

        given(memberRepository.findByLoginId("duplicateId")).willReturn(Optional.of(existingMember));

        assertThatThrownBy(() -> memberService.join(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 존재하는 아이디입니다.");

        verify(memberRepository, never()).save(any(Member.class));
    }

    // ── login 테스트 (신규) ────────────────────────────────────
    @Test
    @DisplayName("로그인 성공 - Access Token과 Refresh Token이 반환된다")
    void login_Success() {
        MemberLoginRequest request = new MemberLoginRequest("testId", "rawPassword");

        // Authentication 모킹
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testId", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        given(authenticationManager.authenticate(any())).willReturn(authentication);

        // Member 조회 모킹
        Member member = Member.builder()
                .loginId("testId")
                .password("encodedPassword")
                .nickname("tester")
                .role(Role.USER)
                .build();
        given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));

        // 토큰 생성 모킹
        given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(any())).willReturn("refreshToken");

        // RefreshToken 없는 경우 (최초 로그인)
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

        String[] tokens = memberService.login(request);

        assertThat(tokens[0]).isEqualTo("accessToken");
        assertThat(tokens[1]).isEqualTo("refreshToken");
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    // ── reissue 테스트 (신규) ──────────────────────────────────
    @Test
    @DisplayName("토큰 재발급 실패 - DB 토큰과 불일치 시 예외 발생 (탈취 감지)")
    void reissue_Fail_TokenMismatch() {
        String requestToken = "stolenToken";

        given(jwtTokenProvider.validateToken(requestToken)).willReturn(true);
        given(jwtTokenProvider.getLoginId(requestToken)).willReturn("testId");

        Member member = Member.builder()
                .loginId("testId")
                .password("encodedPassword")
                .nickname("tester")
                .role(Role.USER)
                .build();
        given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));

        // DB에는 다른 토큰이 저장되어 있음
        RefreshToken savedToken = RefreshToken.builder()
                .member(member)
                .token("originalToken") // 요청 토큰과 다름
                .build();
        given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(savedToken));

        assertThatThrownBy(() -> memberService.reissue(requestToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refresh Token이 일치하지 않습니다.");

        // 탈취 감지 시 DB 토큰 삭제 확인
        verify(refreshTokenRepository, times(1)).deleteByMember(member);
    }
}