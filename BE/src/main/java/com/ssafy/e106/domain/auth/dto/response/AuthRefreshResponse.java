package com.ssafy.e106.domain.auth.dto.response;

import com.ssafy.e106.domain.auth.service.TokenService;

public record AuthRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn) {

  public static AuthRefreshResponse from(TokenService.TokenPair tokenPair) {
    return new AuthRefreshResponse(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.tokenType(),
        tokenPair.expiresIn());
  }
}
