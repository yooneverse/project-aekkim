package com.ssafy.e106.domain.auth.dto.response;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.service.TokenService;

public record AuthLoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    long expiresIn,
    boolean isNewUser,
    AuthenticatedUserResponse user) {

  public static AuthLoginResponse of(TokenService.TokenPair tokenPair, User user, boolean isNewUser) {
    return new AuthLoginResponse(
        tokenPair.accessToken(),
        tokenPair.refreshToken(),
        tokenPair.tokenType(),
        tokenPair.expiresIn(),
        isNewUser,
        AuthenticatedUserResponse.from(user));
  }
}
