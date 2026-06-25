package com.ssafy.e106.domain.finance.dto.response;

public record FinanceDemandDepositAccountCreateResponse(
    Header header,
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
      String bankCode,
      String accountNo,
      Currency currency) {
  }

  public record Currency(
      String currency,
      String currencyName) {
  }
}
