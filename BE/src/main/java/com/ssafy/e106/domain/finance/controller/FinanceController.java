package com.ssafy.e106.domain.finance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.finance.dto.request.FinanceCreditCardTransactionCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceCreditCardTransactionListRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceDemandDepositAccountCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceDemandDepositCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceMemberCreateRequest;
import com.ssafy.e106.domain.finance.dto.request.FinanceMerchantCreateRequest;
import com.ssafy.e106.domain.finance.dto.response.FinanceCardCategoryListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCardIssuerListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCreditCardListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceSignUpCreditCardListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCreditCardTransactionCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceCreditCardTransactionListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositAccountCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositAccountListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceDemandDepositListResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceMemberCreateResponse;
import com.ssafy.e106.domain.finance.dto.response.FinanceMerchantCreateResponse;
import com.ssafy.e106.domain.finance.service.FinanceService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Finance", description = "금융망 연동 API")
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

  private final FinanceService financeService;

  @Operation(summary = "금융망 사용자 계정 생성")
  @PostMapping("/members")
  public ResponseEntity<ApiResponse<FinanceMemberCreateResponse>> createMember(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceMemberCreateRequest request) {
    FinanceMemberCreateResponse response = financeService.createMember(extractUserId(jwt), request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "금융망 사용자 계정을 생성했습니다."));
  }

  @Operation(summary = "금융망 사용자 계정 조회")
  @PostMapping("/members/search")
  public ResponseEntity<ApiResponse<FinanceMemberCreateResponse>> searchMember(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceMemberCreateRequest request) {
    FinanceMemberCreateResponse response = financeService.searchMember(extractUserId(jwt), request);
    return ResponseEntity.ok(ApiResponse.ok(response, "금융망 사용자 계정을 조회했습니다."));
  }

  @Operation(summary = "수시입출금 상품 등록")
  @PostMapping("/demand-deposits")
  public ResponseEntity<ApiResponse<FinanceDemandDepositCreateResponse>> createDemandDeposit(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceDemandDepositCreateRequest request) {
    FinanceDemandDepositCreateResponse response = financeService.createDemandDeposit(
        extractUserId(jwt),
        request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "수시입출금 상품을 등록했습니다."));
  }

  @Operation(summary = "수시입출금 상품 목록 조회")
  @PostMapping("/demand-deposits/list")
  public ResponseEntity<ApiResponse<FinanceDemandDepositListResponse>> inquireDemandDepositList(
      @AuthenticationPrincipal Jwt jwt) {
    FinanceDemandDepositListResponse response = financeService.inquireDemandDepositList(
        extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "수시입출금 상품 목록을 조회했습니다."));
  }

  @Operation(summary = "수시입출금 계좌 생성")
  @PostMapping("/demand-deposit-accounts")
  public ResponseEntity<ApiResponse<FinanceDemandDepositAccountCreateResponse>> createDemandDepositAccount(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceDemandDepositAccountCreateRequest request) {
    FinanceDemandDepositAccountCreateResponse response = financeService.createDemandDepositAccount(
        extractUserId(jwt),
        request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "수시입출금 계좌를 생성했습니다."));
  }

  @Operation(summary = "수시입출금 계좌 목록 조회")
  @PostMapping("/demand-deposit-accounts/list")
  public ResponseEntity<ApiResponse<FinanceDemandDepositAccountListResponse>> inquireDemandDepositAccountList(
      @AuthenticationPrincipal Jwt jwt) {
    FinanceDemandDepositAccountListResponse response = financeService.inquireDemandDepositAccountList(
        extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "수시입출금 계좌 목록을 조회했습니다."));
  }

  @Operation(summary = "카드 카테고리 목록 조회")
  @PostMapping("/credit-cards/categories")
  public ResponseEntity<ApiResponse<FinanceCardCategoryListResponse>> inquireCardCategoryList(
      @AuthenticationPrincipal Jwt jwt) {
    FinanceCardCategoryListResponse response = financeService.inquireCardCategoryList(
        extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "카드 카테고리 목록을 조회했습니다."));
  }

  @Operation(summary = "카드사 목록 조회")
  @PostMapping("/credit-cards/issuers")
  public ResponseEntity<ApiResponse<FinanceCardIssuerListResponse>> inquireCardIssuerList(
      @AuthenticationPrincipal Jwt jwt) {
    FinanceCardIssuerListResponse response = financeService.inquireCardIssuerList(
        extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "카드사 목록을 조회했습니다."));
  }

  @Operation(summary = "카드 상품 목록 조회")
  @PostMapping("/credit-cards/products")
  public ResponseEntity<ApiResponse<FinanceCreditCardListResponse>> inquireCreditCardList(
      @AuthenticationPrincipal Jwt jwt) {
    FinanceCreditCardListResponse response = financeService.inquireCreditCardList(
        extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "카드 상품 목록을 조회했습니다."));
  }

  @Operation(summary = "카드 가맹점 등록")
  @PostMapping("/credit-cards/merchants")
  public ResponseEntity<ApiResponse<FinanceMerchantCreateResponse>> createMerchant(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceMerchantCreateRequest request) {
    FinanceMerchantCreateResponse response = financeService.createMerchant(
        extractUserId(jwt),
        request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "카드 가맹점을 등록했습니다."));
  }

  @Operation(summary = "카드 결제")
  @PostMapping("/credit-cards/transactions")
  public ResponseEntity<ApiResponse<FinanceCreditCardTransactionCreateResponse>> createCreditCardTransaction(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceCreditCardTransactionCreateRequest request) {
    FinanceCreditCardTransactionCreateResponse response = financeService.createCreditCardTransaction(
        extractUserId(jwt),
        request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.ok(response, "카드 결제를 완료했습니다."));
  }

  @Operation(summary = "카드 결제 내역 조회")
  @PostMapping("/credit-cards/transactions/list")
  public ResponseEntity<ApiResponse<FinanceCreditCardTransactionListResponse>> inquireCreditCardTransactionList(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody FinanceCreditCardTransactionListRequest request) {
    FinanceCreditCardTransactionListResponse response = financeService.inquireCreditCardTransactionList(
        extractUserId(jwt),
        request);
    return ResponseEntity.ok(ApiResponse.ok(response, "카드 결제 내역을 조회했습니다."));
  }

  @Operation(summary = "내 카드 목록 조회")
  @PostMapping("/credit-cards/list")
  public ResponseEntity<ApiResponse<FinanceSignUpCreditCardListResponse>> inquireSignUpCreditCardList(
      @AuthenticationPrincipal Jwt jwt) {
    FinanceSignUpCreditCardListResponse response = financeService.inquireSignUpCreditCardList(
        extractUserId(jwt));
    return ResponseEntity.ok(ApiResponse.ok(response, "내 카드 목록을 조회했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
