package com.finance.finportfolio.domain.member.service;

import com.finance.finportfolio.domain.member.dto.LoginResultDto;
import com.finance.finportfolio.domain.member.dto.MemberAdminResponseDto;
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

        @Mock
        private LoginAttemptService loginAttemptService;

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
        @DisplayName("로그인 성공 - Access Token, Refresh Token 및 회원 정보가 반환된다")
        void login_Success() {
                // given
                MemberLoginRequestDto request = new MemberLoginRequestDto("testId", "rawPassword");
                Member member = createMember("testId"); // 닉네임, 역할 등이 포함된 Member 객체

                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "testId", null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));

                given(authenticationManager.authenticate(any())).willReturn(authentication);
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("accessToken");
                given(jwtTokenProvider.createRefreshToken(any())).willReturn("refreshToken");

                // 최초 로그인 시나리오 (기존 토큰 없음)
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.empty());

                // when
                // 리턴 타입 반영: String[] -> LoginResultDto
                LoginResultDto result = memberService.login(request);

                // then
                assertThat(result.accessToken()).isEqualTo("accessToken");
                assertThat(result.refreshToken()).isEqualTo("refreshToken");

                // 추가된 회원 정보(플래그 쿠키용) 검증
                assertThat(result.memberResponseDto().nickname()).isEqualTo(member.getNickname());
                assertThat(result.memberResponseDto().role().name()).isEqualTo("USER");

                // 최초 로그인 시 RefreshToken 엔티티가 새롭게 저장(save)되는지 검증
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
        @DisplayName("로그인 실패 - 잘못된 비밀번호면 실패 카운트 서비스가 호출된다")
        void login_Fail_IncreaseCount() {
                // given
                String loginId = "testId";
                Long fakeMemberId = 1L;
                MemberLoginRequestDto request = new MemberLoginRequestDto(loginId, "wrongPassword");

                // Member 생성 및 Reflection을 통한 ID 주입
                Member member = Member.builder()
                                .loginId(loginId)
                                .build();
                ReflectionTestUtils.setField(member, "id", fakeMemberId);

                given(memberRepository.findByLoginId(loginId)).willReturn(Optional.of(member));
                given(authenticationManager.authenticate(any()))
                                .willThrow(new BadCredentialsException("아이디 또는 비밀번호가 일치하지 않습니다."));

                // when & then
                assertThatThrownBy(() -> memberService.login(request))
                                .isInstanceOf(BadCredentialsException.class);

                // 검증: member.getId()로 전달된 1L이 updateFailCount의 인자로 정확히 호출되었는지 확인
                verify(loginAttemptService, times(1)).updateFailCount(fakeMemberId);

                // 검증: 성공 로직은 실행되지 않아야 함
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
        @DisplayName("토큰 재발급 성공 - 새 토큰과 회원 정보가 반환되고 DB 토큰이 교체된다")
        void reissue_Success() {
                // given
                Member member = createMember("testId");
                RefreshToken savedToken = RefreshToken.builder()
                                .member(member)
                                .token("validRefreshToken")
                                .build();

                given(jwtTokenProvider.validateToken("validRefreshToken")).willReturn(true);
                given(jwtTokenProvider.getLoginId("validRefreshToken")).willReturn("testId");
                given(memberRepository.findByLoginId("testId")).willReturn(Optional.of(member));
                given(refreshTokenRepository.findByMember(member)).willReturn(Optional.of(savedToken));

                // 새 토큰들 생성 Mocking
                given(jwtTokenProvider.createAccessToken(any(), any())).willReturn("newAccessToken");
                given(jwtTokenProvider.createRefreshToken(any())).willReturn("newRefreshToken");

                // when
                // 리턴 타입이 String[]에서 LoginResultDto로 변경됨
                LoginResultDto result = memberService.reissue("validRefreshToken");

                // then
                assertThat(result.accessToken()).isEqualTo("newAccessToken");
                assertThat(result.refreshToken()).isEqualTo("newRefreshToken");

                // 닉네임과 역할 정보도 함께 오는지 확인 (플래그 쿠키 생성용)
                assertThat(result.memberResponseDto().nickname()).isEqualTo(member.getNickname());

                // DB 토큰이 rotate()를 통해 교체되었는지 확인
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

        // ── [관리자] 회원 관리 기능 ─────────────────────────────────────────

        @Test
        @DisplayName("전체 회원 페이징 조회 성공 - 엔티티가 DTO로 정확히 변환된다")
        void getMembersForAdmin_Success() {
                // given
                org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0,
                                10);
                Member member = createMember("testId");
                org.springframework.data.domain.Page<Member> mockPage = new org.springframework.data.domain.PageImpl<>(
                                List.of(member), pageable, 1);

                given(memberRepository.findAll(pageable)).willReturn(mockPage);

                // when
                org.springframework.data.domain.Page<MemberAdminResponseDto> result = memberService
                                .getMembersForAdmin(pageable);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getContent()).hasSize(1);

                MemberAdminResponseDto dto = result.getContent().get(0);
                assertThat(dto.id()).isEqualTo(member.getId());
                assertThat(dto.loginId()).isEqualTo(member.getLoginId());
                assertThat(dto.nickname()).isEqualTo(member.getNickname());
                assertThat(dto.role()).isEqualTo(member.getRole());

                verify(memberRepository, times(1)).findAll(pageable);
        }

        @Test
        @DisplayName("사용자 정지 성공 - 계정이 정지 상태로 변경되고 Refresh Token이 즉시 삭제된다")
        void updateBanStatus_Ban_Success() {
                // given
                Long memberId = 1L;
                Member member = spy(createMember("testId")); // 내부 상태 변경(ban) 검증을 위해 spy 사용

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

                // when
                memberService.updateBanStatus(memberId, true);

                // then
                verify(member, times(1)).ban();
                verify(refreshTokenRepository, times(1)).deleteByMember(member);
                verify(member, never()).unban();
        }

        @Test
        @DisplayName("사용자 정지 해제 성공 - 계정이 활성화 상태로 변경되고 토큰 삭제는 호출되지 않는다")
        void updateBanStatus_Unban_Success() {
                // given
                Long memberId = 1L;
                Member member = spy(createMember("testId"));

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

                // when
                memberService.updateBanStatus(memberId, false);

                // then
                verify(member, times(1)).unban();
                verify(member, never()).ban();
                verify(refreshTokenRepository, never()).deleteByMember(any());
        }

        @Test
        @DisplayName("사용자 임시 잠금 수동 해제 성공 - 로그인 성공 처리 로직이 실행된다")
        void releaseMemberLock_Success() {
                // given
                Long memberId = 1L;
                Member member = spy(createMember("testId"));

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

                // when
                memberService.releaseMemberLock(memberId);

                // then
                verify(member, times(1)).loginSuccess(); // 실패 카운트 초기화 및 잠금 해제 검증
        }
}