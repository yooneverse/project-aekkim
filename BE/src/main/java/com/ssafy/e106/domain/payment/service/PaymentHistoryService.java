package com.ssafy.e106.domain.payment.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.e106.domain.payment.dto.request.PaymentHistoryCreateRequest;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistoryListResponse;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistoryResponse;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistorySaveResponse;
import com.ssafy.e106.domain.payment.entity.PaymentHistory;
import com.ssafy.e106.domain.payment.repository.PaymentHistoryRepository;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

  private final PaymentHistoryRepository paymentHistoryRepository;

  @Transactional
  public PaymentHistorySaveResponse savePaymentHistories(List<PaymentHistoryCreateRequest> requests) {
    if (requests == null || requests.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "결제 이력 목록이 비어 있을 수 없습니다.");
    }

    Map<String, PaymentHistoryCreateRequest> uniqueRequests = new LinkedHashMap<>();
    int duplicateInRequestCount = 0;

    for (PaymentHistoryCreateRequest request : requests) {
      if (uniqueRequests.putIfAbsent(request.paymentId(), request) != null) {
        duplicateInRequestCount++;
      }
    }

    Set<String> existingPaymentIds = paymentHistoryRepository
        .findAllByPaymentIdIn(uniqueRequests.keySet())
        .stream()
        .map(PaymentHistory::getPaymentId)
        .collect(java.util.stream.Collectors.toSet());

    List<PaymentHistory> newPaymentHistories = uniqueRequests.values()
        .stream()
        .filter(request -> !existingPaymentIds.contains(request.paymentId()))
        .map(request -> PaymentHistory.builder()
            .paymentId(request.paymentId())
            .merchantRaw(request.merchantRaw())
            .amount(request.amount())
            .paymentDate(request.paymentDate())
            .category(request.category())
            .build())
        .toList();

    List<PaymentHistoryResponse> savedPayments = paymentHistoryRepository.saveAll(newPaymentHistories)
        .stream()
        .map(PaymentHistoryResponse::of)
        .toList();

    int skippedCount = duplicateInRequestCount + existingPaymentIds.size();
    return new PaymentHistorySaveResponse(
        requests.size(),
        savedPayments.size(),
        skippedCount,
        savedPayments);
  }

  @Transactional(readOnly = true)
  public PaymentHistoryListResponse getPaymentHistories() {
    List<PaymentHistoryResponse> payments = paymentHistoryRepository
        .findAllByOrderByPaymentDateDescPaymentHistoryIdDesc()
        .stream()
        .map(PaymentHistoryResponse::of)
        .toList();

    return new PaymentHistoryListResponse(payments.size(), payments);
  }
}
