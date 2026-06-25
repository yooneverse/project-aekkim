package com.ssafy.e106.domain.notification.batch.model;

public record CheckinCandidate(
    Long userId,
    Long subscriptionId,
    Long serviceId,
    String serviceName,
    String cycleYm) {
}
