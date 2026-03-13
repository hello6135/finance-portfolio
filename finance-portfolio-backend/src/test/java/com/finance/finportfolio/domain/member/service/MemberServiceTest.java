package com.finance.finportfolio.domain.member.service;

import com.finance.finportfolio.domain.member.domain.Member;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;
import com.finance.finportfolio.domain.member.domain.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("회원가입 성공 - 비밀번호가 암호화되어 저장되어야 한다")
    void join_Success() {
        // given
        MemberJoinRequest request = new MemberJoinRequest("testId", "rawPassword", "tester");
        String encodedPassword = "encodedPassword123";

        given(memberRepository.findByLoginId(request.loginId())).willReturn(Optional.empty());
        given(passwordEncoder.encode(request.password())).willReturn(encodedPassword);

        // 가짜 멤버 엔티티 생성 (ID 포함)
        Member savedMember = Member.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .nickname(request.nickname())
                .build();

        ReflectionTestUtils.setField(savedMember, "id", 1L);

        given(memberRepository.save(any(Member.class))).willReturn(savedMember);

        // when
        Long savedId = memberService.join(request);

        // then
        assertThat(savedId).isEqualTo(1L);
        verify(passwordEncoder, times(1)).encode("rawPassword"); // 암호화가 실행되었는지 확인
        verify(memberRepository, times(1)).save(any(Member.class)); // 저장이 실행되었는지 확인
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 아이디인 경우 예외 발생")
    void join_Fail_DuplicateId() {
        // given
        MemberJoinRequest request = new MemberJoinRequest("duplicateId", "password", "tester");
        Member existingMember = Member.builder().loginId("duplicateId").build();

        given(memberRepository.findByLoginId("duplicateId")).willReturn(Optional.of(existingMember));

        // when & then
        assertThatThrownBy(() -> memberService.join(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 존재하는 아이디입니다.");

        // 중복인 경우 저장 로직이 호출되지 않아야 함
        verify(memberRepository, never()).save(any(Member.class));
    }
}