package com.ssafy.e106.domain.subscription.controller;

import org.springframework.http.HttpStatus;
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

import com.ssafy.e106.domain.subscription.dto.request.SubscriptionCreateRequest;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionUpdateRequest;
import com.ssafy.e106.domain.subscription.dto.request.SubscriptionUsageQualificationRequest;
import com.ssafy.e106.domain.subscription.dto.response.BundleDetailResponse;
import com.ssafy.e106.domain.subscription.dto.response.BundleListResponse;
import com.ssafy.e106.domain.subscription.dto.response.ServiceDetailResponse;
import com.ssafy.e106.domain.subscription.dto.response.ServiceListResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionCreateResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionDetailResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionListResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionUpdateResponse;
import com.ssafy.e106.domain.subscription.dto.response.SubscriptionUsageQualificationResponse;
import com.ssafy.e106.domain.subscription.service.SubscriptionService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Subscriptions", description = "구독/서비스 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriptionController {

  private final SubscriptionService subscriptionService;

  @Operation(summary = "서비스 목록 조회")
  @GetMapping("/services")
  public ResponseEntity<ApiResponse<ServiceListResponse>> getServices(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam String category) {
    ServiceListResponse response = subscriptionService.getServices(category);
    return ResponseEntity.ok(ApiResponse.ok(response, "서비스 목록 조회에 성공했습니다."));
  }

  @Operation(summary = "서비스 상세 조회")
  @GetMapping("/services/{serviceId}/plans")
  public ResponseEntity<ApiResponse<ServiceDetailResponse>> getServicePlans(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long serviceId) {
    ServiceDetailResponse response = subscriptionService.getServicePlans(serviceId);
    return ResponseEntity.ok(ApiResponse.ok(response, "서비스 상세 조회에 성공했습니다."));
  }

  @Operation(summary = "번들 목록 조회")
  @GetMapping("/bundles")
  public ResponseEntity<ApiResponse<BundleListResponse>> getBundles(
      @AuthenticationPrincipal Jwt jwt) {
    BundleListResponse response = subscriptionService.getBundles();
    return ResponseEntity.ok(ApiResponse.ok(response, "번들 목록 조회에 성공했습니다."));
  }

  @Operation(summary = "번들 상세 조회")
  @GetMapping("/bundles/{bundleCode}")
  public ResponseEntity<ApiResponse<BundleDetailResponse>> getBundle(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String bundleCode) {
    BundleDetailResponse response = subscriptionService.getBundle(bundleCode);
    return ResponseEntity.ok(ApiResponse.ok(response, "번들 상세 조회에 성공했습니다."));
  }

  @Operation(summary = "구독 등록")
  @PostMapping("/subscriptions")
  public ResponseEntity<ApiResponse<SubscriptionCreateResponse>> createSubscription(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid SubscriptionCreateRequest request) {
    SubscriptionCreateResponse response = subscriptionService.createSubscription(extractUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "구독을 등록했습니다."));
  }

  @Operation(summary = "내 구독 목록 조회")
  @GetMapping("/subscriptions")
  public ResponseEntity<ApiResponse<SubscriptionListResponse>> getSubscriptions(
      @AuthenticationPrincipal Jwt jwt) {
    SubscriptionListResponse response = subscriptionService.getSubscriptions(extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "구독 목록 조회에 성공했습니다."));
  }

  @Operation(summary = "구독 상세 조회")
  @GetMapping("/subscriptions/{subscriptionId}")
  public ResponseEntity<ApiResponse<SubscriptionDetailResponse>> getSubscription(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long subscriptionId) {
    SubscriptionDetailResponse response = subscriptionService.getSubscription(extractUserId(jwt), subscriptionId);
    return ResponseEntity.ok(ApiResponse.ok(response, "구독 상세 조회에 성공했습니다."));
  }

  @Operation(summary = "구독 수정")
  @PatchMapping("/subscriptions/{subscriptionId}")
  public ResponseEntity<ApiResponse<SubscriptionUpdateResponse>> updateSubscription(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long subscriptionId,
      @RequestBody @Valid SubscriptionUpdateRequest request) {
    SubscriptionUpdateResponse response = subscriptionService.updateSubscription(
        extractUserId(jwt),
        subscriptionId,
        request);
    return ResponseEntity.ok(ApiResponse.ok(response, "구독 수정에 성공했습니다."));
  }

  @Operation(summary = "구독 저사용 상태 기록")
  @PostMapping("/subscriptions/usage-qualification")
  public ResponseEntity<ApiResponse<SubscriptionUsageQualificationResponse>> updateUsageQualification(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid SubscriptionUsageQualificationRequest request) {
    SubscriptionUsageQualificationResponse response = subscriptionService.updateUsageQualification(
        extractUserId(jwt),
        request);
    return ResponseEntity.ok(ApiResponse.ok(response, "구독 저사용 상태를 기록했습니다."));
  }

  @Operation(summary = "구독 해지")
  @DeleteMapping("/subscriptions/{subscriptionId}")
  public ResponseEntity<ApiResponse<Void>> deleteSubscription(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long subscriptionId) {
    subscriptionService.deleteSubscription(extractUserId(jwt), subscriptionId);
    return ResponseEntity.ok(ApiResponse.ok(null, "구독 해지에 성공했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
