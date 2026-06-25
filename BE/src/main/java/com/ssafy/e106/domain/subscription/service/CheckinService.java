package com.ssafy.e106.domain.subscription.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.auth.entity.User;
import com.ssafy.e106.domain.auth.repository.UserRepository;
import com.ssafy.e106.domain.subscription.dto.request.CheckinCreateRequest;
import com.ssafy.e106.domain.subscription.dto.response.CheckinCreateResponse;
import com.ssafy.e106.domain.subscription.dto.response.CheckinHistoryItemResponse;
import com.ssafy.e106.domain.subscription.dto.response.CheckinHistoryResponse;
import com.ssafy.e106.domain.subscription.entity.CheckinRecord;
import com.ssafy.e106.domain.subscription.entity.Service;
import com.ssafy.e106.domain.subscription.enums.CheckinResponseType;
import com.ssafy.e106.domain.subscription.repository.CheckinRecordRepository;
import com.ssafy.e106.domain.subscription.repository.ServiceRepository;
import com.ssafy.e106.domain.subscription.repository.SubscriptionRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class CheckinService {

  private static final Pattern CYCLE_YM_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

  private final UserRepository userRepository;
  private final ServiceRepository serviceRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final CheckinRecordRepository checkinRecordRepository;

  @Transactional
  public CheckinCreateResponse createCheckin(Long userId, CheckinCreateRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    Service service = serviceRepository.findById(request.serviceId())
        .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));

    String cycleYm = validateCycleYm(request.cycleYm(), "cycleYm", true);
    CheckinResponseType response = parseResponse(request.response());

    if (!subscriptionRepository.existsByUserIdAndService_ServiceId(userId, request.serviceId())) {
      throw new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND);
    }

    if (checkinRecordRepository.existsByUser_UserIdAndService_ServiceIdAndCycleYm(
        userId,
        request.serviceId(),
        cycleYm)) {
      throw new BusinessException(ErrorCode.CHECKIN_ALREADY_SUBMITTED);
    }

    CheckinRecord checkinRecord = checkinRecordRepository.save(CheckinRecord.builder()
        .user(user)
        .service(service)
        .cycleYm(cycleYm)
        .response(response)
        .respondedAt(LocalDateTime.now())
        .build());

    return CheckinCreateResponse.of(checkinRecord);
  }

  @Transactional(readOnly = true)
  public CheckinHistoryResponse getCheckins(
      Long userId,
      Long serviceId,
      String fromCycleYm,
      String toCycleYm,
      Integer size) {
    userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

    if (serviceId != null) {
      serviceRepository.findById(serviceId)
          .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
    }

    String normalizedFromCycleYm = validateCycleYm(fromCycleYm, "fromCycleYm", false);
    String normalizedToCycleYm = validateCycleYm(toCycleYm, "toCycleYm", false);
    validateCycleYmRange(normalizedFromCycleYm, normalizedToCycleYm);

    int normalizedSize = normalizeSize(size);

    List<CheckinHistoryItemResponse> checkins = checkinRecordRepository.findHistory(
            userId,
            serviceId,
            normalizedFromCycleYm,
            normalizedToCycleYm,
            PageRequest.of(0, normalizedSize))
        .stream()
        .map(CheckinHistoryItemResponse::from)
        .toList();

    return new CheckinHistoryResponse(checkins);
  }

  private String validateCycleYm(String cycleYm, String fieldName, boolean required) {
    if (cycleYm == null || cycleYm.isBlank()) {
      if (required) {
        throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "은 필수입니다.");
      }
      return null;
    }

    if (!CYCLE_YM_PATTERN.matcher(cycleYm).matches()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + "은 YYYY-MM 형식이어야 합니다.");
    }
    return cycleYm;
  }

  private CheckinResponseType parseResponse(String response) {
    if (response == null || response.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "response는 필수입니다.");
    }

    try {
      return CheckinResponseType.from(response);
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "response는 GOOD, BAD, UNKNOWN 중 하나여야 합니다.");
    }
  }

  private void validateCycleYmRange(String fromCycleYm, String toCycleYm) {
    if (fromCycleYm != null && toCycleYm != null && fromCycleYm.compareTo(toCycleYm) > 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "fromCycleYm은 toCycleYm보다 클 수 없습니다.");
    }
  }

  private int normalizeSize(Integer size) {
    if (size == null) {
      return 12;
    }
    if (size < 1) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "size는 1 이상이어야 합니다.");
    }
    return size;
  }
}
