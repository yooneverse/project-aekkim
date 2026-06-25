package com.ssafy.e106.domain.notification.batch.exception;

public class FcmSendBatchException extends RuntimeException {

  public FcmSendBatchException(String message, Throwable cause) {
    super(message, cause);
  }
}
