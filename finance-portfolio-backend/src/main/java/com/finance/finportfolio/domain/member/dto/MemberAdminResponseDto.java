package com.finance.finportfolio.domain.member.dto;

import com.finance.finportfolio.domain.member.entity.Member;
import com.finance.finportfolio.domain.member.entity.Role;

public record MemberAdminResponseDto(
                Long id,
                String loginId,
                String nickname,
                Role role,
                boolean isBanned,
                int loginFailCount) {

        // 내부 생성자
        private MemberAdminResponseDto(Member member) {
                this(
                                member.getId(),
                                member.getLoginId(),
                                member.getNickname(),
                                member.getRole(),
                                member.isBanned(),
                                member.getLoginFailCount());
        }

        // 정적 팩토리 메서드
        public static MemberAdminResponseDto from(Member member) {
                return new MemberAdminResponseDto(member);
        }
}