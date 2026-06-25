package com.ssafy.e106.domain.subscription.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.subscription.dto.request.MerchantMappingBatchConfirmRequest;
import com.ssafy.e106.domain.subscription.dto.request.MerchantMappingBatchLookupRequest;
import com.ssafy.e106.domain.subscription.dto.response.MerchantMappingBatchConfirmResponse;
import com.ssafy.e106.domain.subscription.dto.response.MerchantMappingBatchLookupResponse;
import com.ssafy.e106.domain.subscription.service.MerchantMappingService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "MerchantMappings", description = "가맹점 매핑 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/merchant-mappings")
@RequiredArgsConstructor
public class MerchantMappingController {

  private final MerchantMappingService merchantMappingService;

  @Operation(summary = "가맹점 매칭 배치 조회")
  @PostMapping("/batch-lookup")
  public ResponseEntity<ApiResponse<MerchantMappingBatchLookupResponse>> batchLookup(
      @RequestBody @Valid MerchantMappingBatchLookupRequest request) {
    MerchantMappingBatchLookupResponse response =
        merchantMappingService.batchLookup(request.items());
    return ResponseEntity.ok(ApiResponse.ok(response, "가맹점 매칭 배치 조회에 성공했습니다."));
  }

  @Operation(summary = "가맹점 매핑 배치 확정")
  @PostMapping("/batch-confirm")
  public ResponseEntity<ApiResponse<MerchantMappingBatchConfirmResponse>> batchConfirm(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid MerchantMappingBatchConfirmRequest request) {
    MerchantMappingBatchConfirmResponse response =
        merchantMappingService.batchConfirm(extractUserId(jwt), request.items());
    return ResponseEntity.ok(ApiResponse.ok(response, "가맹점 매핑 배치 확정에 성공했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
