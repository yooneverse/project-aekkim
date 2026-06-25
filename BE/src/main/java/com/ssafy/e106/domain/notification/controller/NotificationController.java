package com.ssafy.e106.domain.notification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.notification.dto.request.UpsertFcmTokenRequest;
import com.ssafy.e106.domain.notification.dto.response.FcmTokenResponse;
import com.ssafy.e106.domain.notification.dto.response.NotificationListResponse;
import com.ssafy.e106.domain.notification.dto.response.NotificationReadResponse;
import com.ssafy.e106.domain.notification.service.NotificationService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Notifications", description = "알림 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "FCM 토큰 등록/갱신")
  @PostMapping("/tokens")
  public ResponseEntity<ApiResponse<FcmTokenResponse>> upsertFcmToken(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid UpsertFcmTokenRequest request) {
    FcmTokenResponse response = notificationService.upsertFcmToken(extractUserId(jwt), request);
    return ResponseEntity.ok(ApiResponse.ok(response, "FCM 토큰이 등록되었습니다."));
  }

  @Operation(summary = "알림 목록 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<NotificationListResponse>> getNotifications(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(defaultValue = "false") boolean unreadOnly,
      @RequestParam(defaultValue = "20") int size) {
    NotificationListResponse response = notificationService.getNotifications(
        extractUserId(jwt),
        unreadOnly,
        size);
    return ResponseEntity.ok(ApiResponse.ok(response, "알림 목록을 조회했습니다."));
  }

  @Operation(summary = "알림 읽음 처리")
  @PatchMapping("/{notificationId}/read")
  public ResponseEntity<ApiResponse<NotificationReadResponse>> markAsRead(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long notificationId) {
    NotificationReadResponse response = notificationService.markAsRead(
        extractUserId(jwt),
        notificationId);
    return ResponseEntity.ok(ApiResponse.ok(response, "알림을 읽음 처리했습니다."));
  }

  @Operation(summary = "알림함 전체 삭제")
  @DeleteMapping
  public ResponseEntity<ApiResponse<Void>> deleteNotifications(
      @AuthenticationPrincipal Jwt jwt) {
    notificationService.deleteNotifications(extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(null, "알림함을 비웠습니다."));
  }

  @Operation(summary = "알림 삭제")
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<ApiResponse<Void>> deleteNotification(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long notificationId) {
    notificationService.deleteNotification(extractUserId(jwt), notificationId);
    return ResponseEntity.ok(ApiResponse.ok(null, "알림을 삭제했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
