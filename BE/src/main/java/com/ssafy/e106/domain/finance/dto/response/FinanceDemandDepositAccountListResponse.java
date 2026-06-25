package com.ssafy.e106.domain.finance.dto.response;

import java.util.List;

public record FinanceDemandDepositAccountListResponse(
    Header header,
    List<Rec> rec) {

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
      String bankName,
      String userName,
      String accountNo,
      String accountName,
      String accountTypeCode,
      String accountTypeName,
      String accountCreatedDate,
      String accountExpiryDate,
      String dailyTransferLimit,
      String oneTimeTransferLimit,
      String accountBalance,
      String lastTransactionDate,
      String currency) {
  }
}
