package com.ssafy.e106.domain.user.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.auth.entity.User;

public record UserMeResponse(
    Long userId,
    String displayName,
    String email,
    String profileImageUrl,
    boolean checkinAlertEnabled,
    boolean promoAlertEnabled,
    boolean optionalConsentAgreed,
    LocalDateTime lastLoginAt) {

  public static UserMeResponse from(User user) {
    return new UserMeResponse(
        user.getUserId(),
        user.getDisplayName(),
        user.getEmail(),
        user.getProfileImageUrl(),
        user.getCheckinAlertEnabled(),
        user.getPromoAlertEnabled(),
        user.getOptionalConsentAgreed(),
        user.getLastLoginAt());
  }
}
