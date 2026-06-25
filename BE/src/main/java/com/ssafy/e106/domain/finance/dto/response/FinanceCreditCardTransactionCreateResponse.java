package com.ssafy.e106.domain.finance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCreditCardTransactionCreateResponse(
    @JsonProperty("Header")
    Header header,
    @JsonProperty("REC")
    Rec rec) {

  public record Header(
      String responseCode,
      String responseMessage,
      String apiName,
      String transmissionDate,
      String transmissionTime,
      String institutionCode,
      String apiKey,
      String apiServiceCode,
      String institutionTransactionUniqueNo) {
  }

  public record Rec(
      String transactionUniqueNo,
      String categoryId,
      String categoryName,
      String merchantId,
      String merchantName,
      String transactionDate,
      String transactionTime,
      String paymentBalance) {
  }
}
