package com.ssafy.e106.domain.admin.dto.response;

public record AdminKakaoUserResetResponse(
    Long userId,
    String provider,
    String providerUserId,
    Long unlinkedKakaoUserId,
    boolean localAccountDeleted
) {
}
