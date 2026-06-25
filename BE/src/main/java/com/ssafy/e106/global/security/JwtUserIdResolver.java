package com.ssafy.e106.global.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

public final class JwtUserIdResolver {

  private JwtUserIdResolver() {
  }

  public static Long resolve(Jwt jwt) {
    if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Access token subject is missing.");
    }

    try {
      return Long.valueOf(jwt.getSubject());
    } catch (NumberFormatException e) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "Access token subject must be a numeric user id.");
    }
  }
}
