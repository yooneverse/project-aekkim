package com.ssafy.e106.domain.admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.e106.domain.admin.service.AdminPaymentService;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistoryListResponse;
import com.ssafy.e106.domain.payment.dto.response.PaymentHistorySaveResponse;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Dev Admin Payments", description = "데모 운영용 결제내역 관리 API")
@RequestMapping("/api/v1/dev/admin/payment-histories")
@RequiredArgsConstructor
public class AdminPaymentController {

  private final AdminPaymentService adminPaymentService;

  @Operation(summary = "결제내역 JSON import")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ApiResponse<PaymentHistorySaveResponse>> importPaymentHistories(
      @RequestPart("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(
            adminPaymentService.importPaymentHistories(file),
            "결제내역 import가 완료되었습니다."));
  }

  @Operation(summary = "관리자 결제내역 조회")
  @GetMapping
  public ResponseEntity<ApiResponse<PaymentHistoryListResponse>> getPaymentHistories() {
    return ResponseEntity.ok(ApiResponse.ok(
        adminPaymentService.getPaymentHistories(),
        "결제내역 조회에 성공했습니다."));
  }
}
