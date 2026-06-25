package com.ssafy.e106.domain.admin.service;

import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.admin.dto.request.AdminNotificationSendRequest;
import com.ssafy.e106.domain.admin.dto.response.AdminNotificationSendResponse;
import com.ssafy.e106.domain.notification.entity.FcmToken;
import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.repository.FcmTokenRepository;
import com.ssafy.e106.domain.notification.service.FcmService;
import com.ssafy.e106.domain.notification.service.NotificationService;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminNotificationService {

  private final FcmTokenRepository fcmTokenRepository;
  private final FcmService fcmService;
  private final NotificationService notificationService;

  @Transactional
  public AdminNotificationSendResponse sendNotification(AdminNotificationSendRequest request) {
    FcmToken fcmToken = fcmTokenRepository.findFirstByUser_UserIdOrderByCreatedAtDesc(request.userId())
        .orElseThrow(() -> new BusinessException(ErrorCode.FCM_TOKEN_NOT_FOUND));

    Notification notification = notificationService.createNotification(
        request.userId(),
        request.type(),
        request.referenceId(),
        request.title().trim(),
        request.body().trim(),
        fcmService.sendNotification(
            fcmToken.getFcmToken(),
            request.title().trim(),
            request.body().trim(),
            request.type(),
            request.referenceId()));

    return new AdminNotificationSendResponse(
        notification.getNotificationId(),
        notification.getSentAt());
  }
}
