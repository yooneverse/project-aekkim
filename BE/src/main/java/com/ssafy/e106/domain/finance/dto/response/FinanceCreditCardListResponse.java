package com.ssafy.e106.domain.finance.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FinanceCreditCardListResponse(
    @JsonProperty("Header")
    Header header,
    @JsonProperty("REC")
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
      String cardUniqueNo,
      String cardIssuerCode,
      String cardIssuerName,
      String cardName,
      String cardTypeCode,
      String cardTypeName,
      String baselinePerformance,
      String maxBenefitLimit,
      String cardDescription,
      List<CardBenefitsInfo> cardBenefitsInfo) {
  }

  public record CardBenefitsInfo(
      String categoryId,
      String categoryName,
      String discountRate) {
  }
}
