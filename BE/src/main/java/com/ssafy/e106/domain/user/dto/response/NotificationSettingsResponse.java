package com.ssafy.e106.domain.user.dto.response;

import com.ssafy.e106.domain.auth.entity.User;

public record NotificationSettingsResponse(
    boolean checkinAlertEnabled,
    boolean promoAlertEnabled) {

  public static NotificationSettingsResponse from(User user) {
    return new NotificationSettingsResponse(
        user.getCheckinAlertEnabled(),
        user.getPromoAlertEnabled());
  }
}
