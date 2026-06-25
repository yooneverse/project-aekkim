package com.ssafy.e106.domain.subscription.enums;

public enum CheckinResponseType {
  GOOD,
  BAD,
  UNKNOWN;

  public static CheckinResponseType from(String value) {
    return CheckinResponseType.valueOf(value.trim().toUpperCase());
  }
}
