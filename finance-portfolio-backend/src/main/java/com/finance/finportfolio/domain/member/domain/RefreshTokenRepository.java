package com.finance.finportfolio.domain.member.domain;

import com.finance.finportfolio.domain.member.domain.Member;
import com.finance.finportfolio.domain.member.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByMember(Member member);

    // 로그아웃 시 토큰값으로 삭제
    void deleteByMember(Member member);
}