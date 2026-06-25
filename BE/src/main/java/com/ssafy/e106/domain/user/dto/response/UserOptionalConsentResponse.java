package com.ssafy.e106.domain.user.dto.response;

import com.ssafy.e106.domain.auth.entity.User;

public record UserOptionalConsentResponse(boolean optionalConsentAgreed) {

  public static UserOptionalConsentResponse from(User user) {
    return new UserOptionalConsentResponse(user.getOptionalConsentAgreed());
  }
}
