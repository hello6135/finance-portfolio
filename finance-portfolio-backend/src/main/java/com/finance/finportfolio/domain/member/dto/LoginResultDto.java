package com.finance.finportfolio.domain.member.dto;

import lombok.Builder;

@Builder
public record LoginResultDto(
        String accessToken,
        String refreshToken,
        MemberResponseDto memberResponseDto) {

}
