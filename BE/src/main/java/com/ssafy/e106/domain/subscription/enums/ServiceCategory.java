package com.ssafy.e106.domain.subscription.enums;

public enum ServiceCategory {
  OTT,
  MUSIC,
  AI;

  public static ServiceCategory from(String value) {
    return ServiceCategory.valueOf(value.trim().toUpperCase());
  }
}
