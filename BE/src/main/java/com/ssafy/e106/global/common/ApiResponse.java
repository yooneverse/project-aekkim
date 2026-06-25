package com.ssafy.e106.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

  private final boolean success;
  private final T data;
  private final String message;

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static <T> ApiResponse<T> ok(T data, String message) {
    return new ApiResponse<>(true, data, message);
  }

  public static ApiResponse<Void> ok() {
    return new ApiResponse<>(true, null, null);
  }
}
