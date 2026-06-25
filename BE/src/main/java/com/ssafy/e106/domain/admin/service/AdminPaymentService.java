package com.ssafy.e106.domain.admin.service;

import java.io.IOException;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.e106.domain.payment.dto.request.PaymentHistoryCreateRequest;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistoryListResponse;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistorySaveResponse;
import com.ssafy.e106.domain.payment.service.PaymentHistoryService;
import com.ssafy.e106.global.exception.BusinessException;
import com.ssafy.e106.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AdminPaymentService {

  private final ObjectMapper objectMapper;
  private final PaymentHistoryService paymentHistoryService;

  @Transactional
  public PaymentHistorySaveResponse importPaymentHistories(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT, "payment import file must not be empty.");
    }

    try (var inputStream = file.getInputStream()) {
      List<PaymentHistoryCreateRequest> requests = objectMapper.readValue(
          inputStream,
          new TypeReference<List<PaymentHistoryCreateRequest>>() {
          });
      return paymentHistoryService.savePaymentHistories(requests);
    } catch (IOException e) {
      throw new BusinessException(
          ErrorCode.INTERNAL_ERROR,
          "failed to read payment import file. fileName="
              + file.getOriginalFilename()
              + ", message="
              + e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public PaymentHistoryListResponse getPaymentHistories() {
    return paymentHistoryService.getPaymentHistories();
  }
}
