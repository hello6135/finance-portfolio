package com.finance.finportfolio.domain.member.dto;

import com.finance.finportfolio.domain.member.entity.Role;

public record MemberResponseDto(
        String nickname,
        Role role) {
}
