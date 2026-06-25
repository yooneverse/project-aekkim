package com.ssafy.e106.domain.finance.dto.response;

public record FinanceDemandDepositCreateResponse(
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
      String accountTypeUniqueNo,
      String bankCode,
      String bankName,
      String accountTypeCode,
      String accountTypeName,
      String accountName,
      String accountDescription,
      String accountType) {
  }
}
