package com.ssafy.e106.domain.notification.batch.model;

import java.util.Set;

public record PromoCandidate(
    Long userId,
    Set<Long> serviceIds) {
}
