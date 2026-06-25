package com.ssafy.e106.domain.finance.dto.response;

public record FinanceMemberCreateResponse(
    String userId,
    String userName,
    String institutionCode,
    String userKey,
    String created,
    String modified) {
}
