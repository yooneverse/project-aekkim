package com.ssafy.e106.domain.payment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.payment.dto.request.PaymentHistoryCreateRequest;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistoryListResponse;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistorySaveResponse;
import com.ssafy.e106.domain.payment.service.PaymentHistoryService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "PaymentHistory", description = "결제 이력 API")
@RequestMapping("/api/v1/payment-history")
@RequiredArgsConstructor
public class PaymentHistoryController {

  private final PaymentHistoryService paymentHistoryService;

  @Operation(summary = "결제 이력 일괄 저장")
  @PostMapping
  public ResponseEntity<ApiResponse<PaymentHistorySaveResponse>> savePaymentHistories(
      @RequestBody @Valid List<@Valid PaymentHistoryCreateRequest> requests) {
    PaymentHistorySaveResponse response = paymentHistoryService.savePaymentHistories(requests);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "결제 이력을 저장했습니다."));
  }

  @Operation(summary = "전체 결제 이력 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<PaymentHistoryListResponse>> getPaymentHistories() {
    PaymentHistoryListResponse response = paymentHistoryService.getPaymentHistories();
    return ResponseEntity.ok(ApiResponse.ok(response, "결제 이력을 조회했습니다."));
  }
}
