package com.ssafy.e106.domain.notification.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.notification.entity.FcmToken;

public record FcmTokenResponse(
    Long fcmTokenId,
    String fcmToken,
    LocalDateTime updatedAt) {

  public static FcmTokenResponse from(FcmToken fcmToken) {
    return new FcmTokenResponse(
        fcmToken.getFcmTokenId(),
        fcmToken.getFcmToken(),
        fcmToken.getUpdatedAt());
  }
}
