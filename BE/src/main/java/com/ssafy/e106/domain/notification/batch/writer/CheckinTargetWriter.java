package com.ssafy.e106.domain.notification.batch.writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.CheckinTarget;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CheckinTargetWriter implements ItemWriter<CheckinTarget> {

  private final NotificationService notificationService;

  @Override
  public void write(Chunk<? extends CheckinTarget> chunk) {
    for (CheckinTarget item : chunk) {
      notificationService.createNotification(
          item.userId(),
          NotificationType.CHECKIN,
          item.subscriptionId(),
          item.title(),
          item.body(),
          null);
    }
  }
}
