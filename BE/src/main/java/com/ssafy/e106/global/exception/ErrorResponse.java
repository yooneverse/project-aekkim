package com.ssafy.e106.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorResponse {

  private final boolean success;
  private final String code;
  private final String message;

  public static ErrorResponse of(ErrorCode errorCode) {
    return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage());
  }

  public static ErrorResponse of(ErrorCode errorCode, String message) {
    return new ErrorResponse(false, errorCode.getCode(), message);
  }
}
