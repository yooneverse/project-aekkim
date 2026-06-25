package com.ssafy.e106.global.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.notification.dto.request.DevNotificationRequest;
import com.ssafy.e106.domain.notification.dto.response.DevNotificationResponse;
import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.service.FcmService;
import com.ssafy.e106.domain.notification.service.NotificationService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Profile({"local", "dev"})
@Validated
@RestController
@Tag(name = "Dev Notifications", description = "FCM 테스트용 API")
@RequestMapping("/api/v1/dev/notifications")
@RequiredArgsConstructor
public class DevNotificationController {

  private final FcmService fcmService;
  private final NotificationService notificationService;

  @Operation(summary = "FCM 테스트 알림 발송")
  @PostMapping("/test")
  public ResponseEntity<ApiResponse<DevNotificationResponse>> sendTestNotification(
      @RequestBody @Valid DevNotificationRequest request) {
    DevNotificationResponse response = fcmService.sendTestNotification(
        request.token(),
        request.title(),
        request.body(),
        request.type(),
        request.referenceId());

    Notification notification = notificationService.createNotification(
        request.userId(),
        request.type(),
        request.referenceId(),
        request.title(),
        request.body(),
        response.sentAt());

    return ResponseEntity.ok(ApiResponse.ok(
        response.withNotificationId(notification.getNotificationId()),
        "테스트 알림이 발송되었습니다."));
  }
}
