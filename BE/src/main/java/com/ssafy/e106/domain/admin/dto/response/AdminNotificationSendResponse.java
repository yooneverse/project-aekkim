package com.ssafy.e106.domain.admin.dto.response;

import java.time.LocalDateTime;

public record AdminNotificationSendResponse(
    Long notificationId,
    LocalDateTime sentAt) {
}
