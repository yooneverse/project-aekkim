package com.ssafy.e106.domain.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.admin.dto.request.AdminNotificationSendRequest;
import com.ssafy.e106.domain.admin.dto.response.AdminNotificationSendResponse;
import com.ssafy.e106.domain.admin.service.AdminNotificationService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Dev Admin Notifications", description = "데모 운영용 관리자 알림 API")
@RequestMapping("/api/v1/dev/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

  private final AdminNotificationService adminNotificationService;

  @Operation(summary = "관리자 수동 알림 발송")
  @PostMapping
  public ResponseEntity<ApiResponse<AdminNotificationSendResponse>> sendNotification(
      @RequestBody @Valid AdminNotificationSendRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(
            adminNotificationService.sendNotification(request),
            "관리자 수동 알림 발송에 성공했습니다."));
  }
}
