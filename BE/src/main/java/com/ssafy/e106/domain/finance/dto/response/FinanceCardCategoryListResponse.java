package com.ssafy.e106.domain.finance.dto.response;

import java.util.List;

public record FinanceCardCategoryListResponse(
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
      String categoryId,
      String categoryName,
      String categoryDescription) {
  }
}
