package com.ssafy.e106.domain.finance.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCreditCardTransactionListResponse(
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
      String cardIssuerCode,
      String cardIssuerName,
      String cardName,
      String cardNo,
      String estimatedBalance,
      List<Transaction> transactionList) {
  }

  public record Transaction(
      String transactionUniqueNo,
      String categoryId,
      String categoryName,
      String merchantId,
      String merchantName,
      String transactionDate,
      String transactionTime,
      String transactionBalance,
      String cardStatus,
      String billStatementsYn,
      String billStatementsStatus) {
  }
}
