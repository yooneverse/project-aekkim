package com.ssafy.e106.domain.notification.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.PromoSendTarget;
import com.ssafy.e106.domain.notification.entity.FcmToken;
import com.ssafy.e106.domain.notification.entity.Notification;
import com.ssafy.e106.domain.notification.repository.FcmTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromoNotificationSendProcessor implements ItemProcessor<Notification, PromoSendTarget> {

  private final FcmTokenRepository fcmTokenRepository;

  @Override
  public PromoSendTarget process(Notification item) {
    FcmToken fcmToken = fcmTokenRepository.findFirstByUser_UserIdOrderByCreatedAtDesc(
        item.getUser().getUserId())
        .orElse(null);

    if (fcmToken == null) {
      return null;
    }

    return new PromoSendTarget(
        item.getNotificationId(),
        buildDedupKey(item),
        fcmToken.getFcmToken(),
        item.getReferenceId(),
        item.getTitle(),
        item.getBody());
  }

  private String buildDedupKey(Notification item) {
    return item.getUser().getUserId() + ":" + item.getReferenceId();
  }
}
