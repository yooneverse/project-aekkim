package com.ssafy.e106.domain.notification.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.enums.NotificationType;

public record NotificationListItemResponse(
    Long notificationId,
    NotificationType type,
    Long referenceId,
    String title,
    String body,
    boolean isRead,
    LocalDateTime readAt,
    LocalDateTime sentAt) {

  public static NotificationListItemResponse from(Notification notification) {
    return new NotificationListItemResponse(
        notification.getNotificationId(),
        notification.getType(),
        notification.getReferenceId(),
        notification.getTitle(),
        notification.getBody(),
        notification.getReadAt() != null,
        notification.getReadAt(),
        notification.getSentAt());
  }
}
