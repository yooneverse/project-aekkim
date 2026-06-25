package com.ssafy.e106.domain.admin.dto.request;

import com.ssafy.e106.domain.notification.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminNotificationSendRequest(
    @NotNull(message = "userId는 필수입니다.")
    Long userId,

    @NotNull(message = "type은 필수입니다.")
    NotificationType type,

    Long referenceId,

    @NotBlank(message = "title은 필수입니다.")
    String title,

    @NotBlank(message = "body는 필수입니다.")
    String body) {
}
