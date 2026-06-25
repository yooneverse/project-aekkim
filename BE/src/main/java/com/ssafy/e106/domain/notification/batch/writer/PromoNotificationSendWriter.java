package com.ssafy.e106.domain.notification.batch.writer;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.PromoSendTarget;
import com.ssafy.e106.domain.notification.batch.service.NotificationBatchRedisService;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.service.FcmService;
import com.ssafy.e106.domain.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromoNotificationSendWriter implements ItemWriter<PromoSendTarget> {

  private static final Duration PROMO_SEND_LOCK_TTL = Duration.ofMinutes(10);
  private static final Duration PROMO_SENT_TTL = Duration.ofDays(30);

  private final FcmService fcmService;
  private final NotificationService notificationService;
  private final NotificationBatchRedisService notificationBatchRedisService;

  @Override
  public void write(Chunk<? extends PromoSendTarget> chunk) {
    for (PromoSendTarget item : chunk) {
      if (notificationBatchRedisService.isPromoAlreadySent(item.dedupKey())) {
        notificationService.markAsSent(item.notificationId(), LocalDateTime.now());
        continue;
      }

      if (!notificationBatchRedisService.tryAcquirePromoSendLock(
          item.dedupKey(),
          PROMO_SEND_LOCK_TTL)) {
        continue;
      }

      try {
        LocalDateTime sentAt = fcmService.sendNotification(
            item.token(),
            item.title(),
            item.body(),
            NotificationType.PROMO,
            item.referenceId());

        notificationBatchRedisService.markPromoSent(item.dedupKey(), PROMO_SENT_TTL);
        notificationService.markAsSent(item.notificationId(), sentAt);
      } finally {
        notificationBatchRedisService.releasePromoSendLock(item.dedupKey());
      }
    }
  }
}
