package com.ssafy.e106.domain.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.user.dto.request.UpdateNotificationSettingsRequest;
import com.ssafy.e106.domain.user.dto.request.UpdateOptionalConsentRequest;
import com.ssafy.e106.domain.user.dto.response.NotificationSettingsResponse;
import com.ssafy.e106.domain.user.dto.response.UserMeResponse;
import com.ssafy.e106.domain.user.dto.response.UserOptionalConsentResponse;
import com.ssafy.e106.domain.user.service.UserService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Users", description = "사용자 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @Operation(summary = "내 정보 조회")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserMeResponse>> getMyInfo(
      @AuthenticationPrincipal Jwt jwt) {
    UserMeResponse response = userService.getMyInfo(extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "내 정보 조회에 성공했습니다."));
  }

  @Operation(summary = "회원 탈퇴")
  @DeleteMapping("/me")
  public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
      @AuthenticationPrincipal Jwt jwt) {
    userService.deleteMyAccount(extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(null, "회원 탈퇴에 성공했습니다."));
  }

  @Operation(summary = "알림 설정 변경")
  @PatchMapping("/me/notification-settings")
  public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateNotificationSettings(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid UpdateNotificationSettingsRequest request) {
    NotificationSettingsResponse response = userService.updateNotificationSettings(
        extractUserId(jwt),
        request);
    return ResponseEntity.ok(ApiResponse.ok(
        response,
        "알림 설정이 변경되었습니다."));
  }

  @Operation(summary = "선택 사항 동의 변경")
  @PatchMapping("/me/optional-consent")
  public ResponseEntity<ApiResponse<UserOptionalConsentResponse>> updateOptionalConsent(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody @Valid UpdateOptionalConsentRequest request) {
    UserOptionalConsentResponse response = userService.updateOptionalConsent(
        extractUserId(jwt),
        request);
    return ResponseEntity.ok(ApiResponse.ok(
        response,
        "선택 사항 동의가 변경되었습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
