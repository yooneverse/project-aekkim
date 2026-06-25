package com.ssafy.e106.domain.notification.batch.model;

public record PromoTarget(
    Long userId,
    Long promotionId,
    String title,
    String body) {
}
