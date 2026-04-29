package com.finance.finportfolio.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {
    private final MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW) // 독립적인 새 트랜잭션 생성
    public void updateFailCount(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        member.loginFailed();
    }
}