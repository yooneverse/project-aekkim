package com.ssafy.e106.domain.notification.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.notification.entity.Notification;

public record NotificationReadResponse(
    Long notificationId,
    boolean isRead,
    LocalDateTime readAt) {

  public static NotificationReadResponse from(Notification notification) {
    return new NotificationReadResponse(
        notification.getNotificationId(),
        notification.getReadAt() != null,
        notification.getReadAt());
  }
}
