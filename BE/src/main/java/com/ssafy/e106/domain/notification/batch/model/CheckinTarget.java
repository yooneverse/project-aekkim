package com.ssafy.e106.domain.notification.batch.model;

public record CheckinTarget(
    Long userId,
    Long subscriptionId,
    String title,
    String body) {
}
