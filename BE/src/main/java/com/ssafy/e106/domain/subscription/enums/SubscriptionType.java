package com.ssafy.e106.domain.subscription.enums;

public enum SubscriptionType {
  SINGLE,
  BUNDLE;

  public static SubscriptionType from(String value) {
    if (value == null || value.isBlank()) {
      return SINGLE;
    }
    return SubscriptionType.valueOf(value.trim().toUpperCase());
  }
}
