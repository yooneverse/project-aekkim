package com.ssafy.e106.domain.notification.batch.writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.PromoTarget;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromoTargetWriter implements ItemWriter<PromoTarget> {

  private final NotificationService notificationService;

  @Override
  public void write(Chunk<? extends PromoTarget> chunk) {
    for (PromoTarget item : chunk) {
      notificationService.createNotification(
          item.userId(),
          NotificationType.PROMO,
          item.promotionId(),
          item.title(),
          item.body(),
          null);
    }
  }
}
