package com.finance.finportfolio.domain.member.service;

import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.entity.Role;
import com.finance.finportfolio.domain.member.repository.MemberRepository;
import com.finance.finportfolio.domain.member.service.CustomUserDetailsService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class) // Mockito 환경 설정
class CustomUserDetailsServiceTest {

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("로그인 아이디로 유저 정보를 정확히 불러오는지 확인")
    void loadUserByUsername_Success() {
        // given (준비)
        Member member = Member.builder()
                .loginId("testUser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        given(memberRepository.findByLoginId("testUser")).willReturn(Optional.of(member));

        // when (실행)
        UserDetails userDetails = userDetailsService.loadUserByUsername("testUser");

        // then (검증)
        assertThat(userDetails.getUsername()).isEqualTo("testUser");
        assertThat(userDetails.getPassword()).isEqualTo("encodedPassword");
        assertThat(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"))).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 아이디로 조회 시 예외가 발생하는지 확인")
    void loadUserByUsername_NotFound() {
        // given
        given(memberRepository.findByLoginId(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknownUser"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("아이디를 찾을 수 없습니다.");
    }
}