package com.ssafy.e106.domain.notification.dto.response;

import java.time.LocalDateTime;

public record DevNotificationResponse(
    Long notificationId,
    String messageId,
    LocalDateTime sentAt) {

  public DevNotificationResponse withNotificationId(Long notificationId) {
    return new DevNotificationResponse(notificationId, messageId, sentAt);
  }
}
