package com.ssafy.e106.domain.notification.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpsertFcmTokenRequest(
    @NotBlank(message = "fcmToken은 필수입니다.")
    String fcmToken) {
}
