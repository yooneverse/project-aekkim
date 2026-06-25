package com.ssafy.e106.domain.subscription.controller;

import org.springframework.http.HttpStatus;
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

import com.ssafy.e106.domain.subscription.dto.request.CheckinCreateRequest;
import com.ssafy.e106.domain.subscription.dto.response.CheckinCreateResponse;
import com.ssafy.e106.domain.subscription.dto.response.CheckinHistoryResponse;
import com.ssafy.e106.domain.subscription.service.CheckinService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Checkins", description = "체크인 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/checkins")
@RequiredArgsConstructor
public class CheckinController {

  private final CheckinService checkinService;

  @Operation(summary = "체크인 제출")
  @PostMapping
  public ResponseEntity<ApiResponse<CheckinCreateResponse>> createCheckin(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid CheckinCreateRequest request) {
    CheckinCreateResponse response = checkinService.createCheckin(extractUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "체크인 응답을 저장했습니다."));
  }

  @Operation(summary = "체크인 이력 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<CheckinHistoryResponse>> getCheckins(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) Long serviceId,
      @RequestParam(required = false) String fromCycleYm,
      @RequestParam(required = false) String toCycleYm,
      @RequestParam(required = false) Integer size) {
    CheckinHistoryResponse response = checkinService.getCheckins(
        extractUserId(jwt),
        serviceId,
        fromCycleYm,
        toCycleYm,
        size);
    return ResponseEntity.ok(ApiResponse.ok(response, "체크인 이력 조회에 성공했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
