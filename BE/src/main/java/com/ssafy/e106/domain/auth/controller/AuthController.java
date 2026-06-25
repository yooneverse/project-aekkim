package com.ssafy.e106.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.auth.dto.request.GoogleLoginRequest;
import com.ssafy.e106.domain.auth.dto.request.KakaoLoginRequest;
import com.ssafy.e106.domain.auth.dto.request.RefreshTokenRequest;
import com.ssafy.e106.domain.auth.dto.response.AuthLoginResponse;
import com.ssafy.e106.domain.auth.dto.response.AuthRefreshResponse;
import com.ssafy.e106.domain.auth.service.AuthService;
import com.ssafy.e106.domain.auth.service.TokenService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Auth", description = "인증 API")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @Operation(summary = "카카오 로그인")
  @PostMapping("/login/kakao")
  public ResponseEntity<ApiResponse<AuthLoginResponse>> loginWithKakao(
      @RequestBody @Valid KakaoLoginRequest request) {
    AuthService.LoginResult loginResult = authService.loginWithKakao(request.kakaoAccessToken());
    return ResponseEntity.ok(ApiResponse.ok(
        AuthLoginResponse.of(loginResult.tokenPair(), loginResult.user(), loginResult.isNewUser()),
        "로그인에 성공했습니다."));
  }

  @Operation(summary = "구글 로그인")
  @PostMapping("/login/google")
  public ResponseEntity<ApiResponse<AuthLoginResponse>> loginWithGoogle(
      @RequestBody @Valid GoogleLoginRequest request) {
    AuthService.LoginResult loginResult = authService.loginWithGoogle(request.googleIdToken());
    return ResponseEntity.ok(ApiResponse.ok(
        AuthLoginResponse.of(loginResult.tokenPair(), loginResult.user(), loginResult.isNewUser()),
        "로그인에 성공했습니다."));
  }

  @Operation(summary = "액세스 토큰 재발급")
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthRefreshResponse>> refresh(
      @RequestBody @Valid RefreshTokenRequest request) {
    TokenService.TokenPair tokenPair = authService.refresh(request.refreshToken());
    return ResponseEntity.ok(ApiResponse.ok(
        AuthRefreshResponse.from(tokenPair),
        "액세스 토큰이 재발급되었습니다."));
  }

  @Operation(summary = "로그아웃")
  @SecurityRequirement(name = "bearerAuth")
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt) {
    authService.logout(JwtUserIdResolver.resolve(jwt));
    return ResponseEntity.ok(
        ApiResponse.ok(null, "로그아웃에 성공했습니다."));
  }
}
