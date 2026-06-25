package com.ssafy.e106.domain.subscriptionusage.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.subscriptionusage.dto.request.SubscriptionUsageDailyUpsertRequest;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageDailyResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageDailyUpsertResponse;
import com.ssafy.e106.domain.subscriptionusage.dto.response.SubscriptionUsageReportResponse;
import com.ssafy.e106.domain.subscriptionusage.service.SubscriptionUsageService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Subscription Usage", description = "구독 사용량 집계 및 인사이트 API")
@RequestMapping("/api/v1/subscription-usages")
@RequiredArgsConstructor
public class SubscriptionUsageController {

  private final SubscriptionUsageService subscriptionUsageService;

  @Operation(summary = "일별 사용량 업로드")
  @PostMapping("/daily")
  public ResponseEntity<ApiResponse<SubscriptionUsageDailyUpsertResponse>> upsertDailyUsage(
      @Valid @RequestBody SubscriptionUsageDailyUpsertRequest request) {
    return ResponseEntity.ok(ApiResponse.ok(
        subscriptionUsageService.upsertDailyUsage(request),
        "일별 사용량 집계를 저장했습니다."));
  }

  @Operation(summary = "구독 사용량 리포트 조회")
  @GetMapping("/report")
  public ResponseEntity<ApiResponse<SubscriptionUsageReportResponse>> getUsageReport(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "조회 기간(일), 기본값 30")
      @RequestParam(required = false) Integer days) {
    return ResponseEntity.ok(ApiResponse.ok(
        subscriptionUsageService.getUsageReport(extractUserId(jwt), days),
        "구독 사용량 리포트 조회에 성공했습니다."));
  }

  @Operation(summary = "일별 사용량 흐름 조회")
  @GetMapping("/daily")
  public ResponseEntity<ApiResponse<SubscriptionUsageDailyResponse>> getDailyUsage(
      @AuthenticationPrincipal Jwt jwt,
      @Parameter(description = "조회 기간(일), 기본값 30")
      @RequestParam(required = false) Integer days,
      @Parameter(description = "특정 구독 필터, 없으면 전체 구독 사용량 합계")
      @RequestParam(required = false) Long subscriptionId) {
    return ResponseEntity.ok(ApiResponse.ok(
        subscriptionUsageService.getDailyUsage(extractUserId(jwt), days, subscriptionId),
        "일별 사용량 흐름 조회에 성공했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }
}
