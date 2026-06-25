package com.ssafy.e106.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminUserViewResponse(
    Long userId,
    String provider,
    String providerUserId,
    String email,
    String displayName,
    Boolean checkinAlertEnabled,
    Boolean promoAlertEnabled,
    Boolean optionalConsentAgreed,
    LocalDateTime connectedAt,
    LocalDateTime lastLoginAt
) {
}
