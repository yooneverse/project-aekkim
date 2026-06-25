package com.ssafy.e106.domain.notification.batch.model;

public record PromoSendTarget(
    Long notificationId,
    String dedupKey,
    String token,
    Long referenceId,
    String title,
    String body) {
}
