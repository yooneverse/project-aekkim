package com.ssafy.e106.domain.notification.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.ssafy.e106.domain.notification.batch.exception.FcmSendBatchException;
import com.ssafy.e106.domain.notification.dto.response.DevNotificationResponse;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

  private final FirebaseMessaging firebaseMessaging;

  public LocalDateTime sendNotification(
      String token,
      String title,
      String body,
      NotificationType type,
      Long referenceId) {
    Message message = buildMessage(token, title, body, type, referenceId);
    try {
      String messageId = firebaseMessaging.send(message);
      LocalDateTime sentAt = LocalDateTime.now();
      log.info("FCM notification sent. messageId={}", messageId);
      return sentAt;
    } catch (FirebaseMessagingException e) {
      log.error("Failed to send FCM notification. errorCode={}, message={}",
          e.getErrorCode(),
          e.getMessage(),
          e);
      throw new FcmSendBatchException("FCM notification send failed.", e);
    }
  }

  public DevNotificationResponse sendTestNotification(
      String token,
      String title,
      String body,
      NotificationType type,
      Long referenceId) {
    try {
      LocalDateTime sentAt = sendNotification(token, title, body, type, referenceId);
      return new DevNotificationResponse(null, null, sentAt);
    } catch (FcmSendBatchException e) {
      throw new BusinessException(ErrorCode.FCM_SEND_FAILED);
    }
  }

  private Message buildMessage(
      String token,
      String title,
      String body,
      NotificationType type,
      Long referenceId) {
    Message.Builder builder = Message.builder()
        .setToken(token)
        .putData("type", type.name())
        .putData("title", title)
        .putData("body", body);

    if (referenceId != null) {
      builder.putData("referenceId", String.valueOf(referenceId));
    }

    return builder.build();
  }
}
