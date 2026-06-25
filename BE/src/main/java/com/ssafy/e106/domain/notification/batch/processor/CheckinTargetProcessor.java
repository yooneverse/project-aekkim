package com.ssafy.e106.domain.notification.batch.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.ssafy.e106.domain.notification.batch.model.CheckinCandidate;
import com.ssafy.e106.domain.notification.batch.model.CheckinTarget;
import com.ssafy.e106.domain.notification.enums.NotificationType;
import com.ssafy.e106.domain.notification.repository.NotificationRepository;
import com.ssafy.e106.domain.subscription.repository.CheckinRecordRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CheckinTargetProcessor implements ItemProcessor<CheckinCandidate, CheckinTarget> {

  private final CheckinRecordRepository checkinRecordRepository;
  private final NotificationRepository notificationRepository;

  @Override
  public CheckinTarget process(CheckinCandidate item) {
    if (checkinRecordRepository.existsByUser_UserIdAndService_ServiceIdAndCycleYm(
        item.userId(),
        item.serviceId(),
        item.cycleYm())) {
      return null;
    }

    String title = "이 서비스 사용 중인가요?";
    String body = item.serviceName() + " 구독 이용 여부를 확인해 주세요. (" + item.cycleYm() + ")";

    if (notificationRepository.existsByUser_UserIdAndTypeAndReferenceIdAndBody(
        item.userId(),
        NotificationType.CHECKIN,
        item.subscriptionId(),
        body)) {
      return null;
    }

    return new CheckinTarget(
        item.userId(),
        item.subscriptionId(),
        title,
        body);
  }
}
