package com.ssafy.e106.domain.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingsRequest(
    @NotNull(message = "체크인 알림 수신 여부는 필수입니다.")
    Boolean checkinAlertEnabled,
    @NotNull(message = "프로모션 알림 수신 여부는 필수입니다.")
    Boolean promoAlertEnabled) {
}
