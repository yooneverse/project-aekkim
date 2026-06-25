package com.ssafy.e106.domain.auth.dto.response;

import com.ssafy.e106.domain.auth.entity.User;

public record AuthenticatedUserResponse(
    Long userId,
    String displayName,
    String email,
    String profileImageUrl,
    boolean checkinAlertEnabled,
    boolean promoAlertEnabled,
    boolean optionalConsentAgreed) {

  public static AuthenticatedUserResponse from(User user) {
    return new AuthenticatedUserResponse(
        user.getUserId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getProfileImageUrl(),
        user.getCheckinAlertEnabled(),
        user.getPromoAlertEnabled(),
        user.getOptionalConsentAgreed());
  }
}
