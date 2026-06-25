package com.ssafy.e106.domain.subscription.dto.response;

import java.time.LocalDateTime;

import com.ssafy.e106.domain.subscription.entity.CheckinRecord;

public record CheckinCreateResponse(
    Long checkinRecordId,
    Long serviceId,
    String cycleYm,
    String response,
    LocalDateTime respondedAt) {

  public static CheckinCreateResponse of(CheckinRecord checkinRecord) {
    return new CheckinCreateResponse(
        checkinRecord.getCheckinRecordId(),
        checkinRecord.getService().getServiceId(),
        checkinRecord.getCycleYm(),
        checkinRecord.getResponse().name(),
        checkinRecord.getRespondedAt());
  }
}
