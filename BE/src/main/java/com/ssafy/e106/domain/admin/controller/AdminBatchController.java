package com.ssafy.e106.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.admin.dto.response.AdminBatchExecutionResponse;
import com.ssafy.e106.domain.admin.service.AdminBatchService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Dev Admin Batch", description = "데모 운영용 관리자 배치 실행 API")
@RequestMapping("/api/v1/dev/admin/batch-executions")
@RequiredArgsConstructor
public class AdminBatchController {

  private final AdminBatchService adminBatchService;

  @Operation(summary = "체크인 대상 선정 배치 수동 실행")
  @PostMapping("/checkin-target-selection")
  public ResponseEntity<ApiResponse<AdminBatchExecutionResponse>> runCheckinTargetSelection() {
    return ResponseEntity.ok(ApiResponse.ok(
        adminBatchService.runJob("checkin-target-selection"),
        "체크인 대상 선정 배치 실행을 요청했습니다."));
  }

  @Operation(summary = "체크인 알림 발송 배치 수동 실행")
  @PostMapping("/checkin-notification-send")
  public ResponseEntity<ApiResponse<AdminBatchExecutionResponse>> runCheckinNotificationSend() {
    return ResponseEntity.ok(ApiResponse.ok(
        adminBatchService.runJob("checkin-notification-send"),
        "체크인 알림 발송 배치 실행을 요청했습니다."));
  }

  @Operation(summary = "프로모션 대상 선정 배치 수동 실행")
  @PostMapping("/promo-target-selection")
  public ResponseEntity<ApiResponse<AdminBatchExecutionResponse>> runPromoTargetSelection() {
    return ResponseEntity.ok(ApiResponse.ok(
        adminBatchService.runJob("promo-target-selection"),
        "프로모션 대상 선정 배치 실행을 요청했습니다."));
  }

  @Operation(summary = "프로모션 알림 발송 배치 수동 실행")
  @PostMapping("/promo-notification-send")
  public ResponseEntity<ApiResponse<AdminBatchExecutionResponse>> runPromoNotificationSend() {
    return ResponseEntity.ok(ApiResponse.ok(
        adminBatchService.runJob("promo-notification-send"),
        "프로모션 알림 발송 배치 실행을 요청했습니다."));
  }
}
