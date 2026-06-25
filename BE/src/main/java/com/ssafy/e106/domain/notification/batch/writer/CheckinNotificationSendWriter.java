package com.ssafy.e106.domain.notification.batch.writer;

import java.time.LocalDateTime;
import java.time.Duration;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.CheckinSendTarget;
import com.ssafy.e106.domain.notification.batch.service.NotificationBatchRedisService;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.service.FcmService;
import com.ssafy.e106.domain.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CheckinNotificationSendWriter implements ItemWriter<CheckinSendTarget> {

  private static final Duration CHECKIN_SEND_LOCK_TTL = Duration.ofMinutes(10);
  private static final Duration CHECKIN_SENT_TTL = Duration.ofDays(40);

  private final FcmService fcmService;
  private final NotificationService notificationService;
  private final NotificationBatchRedisService notificationBatchRedisService;

  @Override
  public void write(Chunk<? extends CheckinSendTarget> chunk) {
    for (CheckinSendTarget item : chunk) {
      if (notificationBatchRedisService.isCheckinAlreadySent(item.dedupKey())) {
        notificationService.markAsSent(item.notificationId(), LocalDateTime.now());
        continue;
      }

      if (!notificationBatchRedisService.tryAcquireCheckinSendLock(
          item.dedupKey(),
          CHECKIN_SEND_LOCK_TTL)) {
        continue;
      }

      try {
        LocalDateTime sentAt = fcmService.sendNotification(
            item.token(),
            item.title(),
            item.body(),
            NotificationType.CHECKIN,
            item.referenceId());

        notificationBatchRedisService.markCheckinSent(item.dedupKey(), CHECKIN_SENT_TTL);
        notificationService.markAsSent(item.notificationId(), sentAt);
      } finally {
        notificationBatchRedisService.releaseCheckinSendLock(item.dedupKey());
      }
    }
  }
}
