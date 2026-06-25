package com.ssafy.e106.domain.subscription.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.subscription.entity.CheckinRecord;

public record CheckinHistoryItemResponse(
    Long checkinRecordId,
    Long serviceId,
    String serviceName,
    String cycleYm,
    String response,
    LocalDateTime respondedAt) {

  public static CheckinHistoryItemResponse from(CheckinRecord checkinRecord) {
    return new CheckinHistoryItemResponse(
        checkinRecord.getCheckinRecordId(),
        checkinRecord.getService().getServiceId(),
        checkinRecord.getService().getName(),
        checkinRecord.getCycleYm(),
        checkinRecord.getResponse().name(),
        checkinRecord.getRespondedAt());
  }
}
