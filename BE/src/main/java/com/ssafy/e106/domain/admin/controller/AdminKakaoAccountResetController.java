package com.ssafy.e106.domain.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.admin.dto.response.AdminKakaoUserResetResponse;
import com.ssafy.e106.domain.admin.service.AdminKakaoAccountResetService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Dev Admin Kakao Reset", description = "개발 환경 카카오 계정 강제 unlink 및 로컬 계정 삭제 API")
@RequestMapping("/api/v1/dev/admin")
@RequiredArgsConstructor
public class AdminKakaoAccountResetController {

  private final AdminKakaoAccountResetService adminKakaoAccountResetService;

  @Operation(summary = "특정 카카오 사용자 강제 unlink 후 로컬 계정 삭제")
  @PostMapping("/users/{userId}/kakao-reset")
  public ResponseEntity<ApiResponse<AdminKakaoUserResetResponse>> resetKakaoUser(
      @PathVariable Long userId) {
    AdminKakaoUserResetResponse response = adminKakaoAccountResetService.resetKakaoUser(userId);
    return ResponseEntity.ok(ApiResponse.ok(response, "카카오 unlink 및 로컬 계정 삭제를 완료했습니다."));
  }
}
