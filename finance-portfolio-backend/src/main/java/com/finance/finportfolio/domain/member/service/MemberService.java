package com.finance.finportfolio.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.member.domain.Member;
import com.finance.finportfolio.domain.member.domain.MemberRepository;
import com.finance.finportfolio.domain.member.domain.Role;
import com.finance.finportfolio.domain.member.dto.MemberJoinRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long join(MemberJoinRequest request) {
        memberRepository.findByLoginId(request.loginId())
                .ifPresent(m -> {
                    throw new IllegalStateException("이미 존재하는 아이디입니다.");
                });

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.builder()
                .loginId(request.loginId())
                .password(encodedPassword)
                .nickname(request.nickname())
                .role(Role.USER)
                .build();

        return memberRepository.save(member).getId();
    }
}
