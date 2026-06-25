package com.ssafy.e106.global.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
    log.warn("BusinessException: {}", e.getMessage());
    ErrorCode errorCode = e.getErrorCode();
    String message = e.getMessage() == null || e.getMessage().isBlank()
        ? errorCode.getMessage()
        : e.getMessage();
    return ResponseEntity
        .status(errorCode.getHttpStatus())
        .body(ErrorResponse.of(errorCode, message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(
      MethodArgumentNotValidException e) {
    String message = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));
    log.warn("Validation failed: {}", message);
    return ResponseEntity
        .badRequest()
        .body(new ErrorResponse(false, "INVALID_INPUT", message));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    log.error("Unhandled exception: ", e);
    return ResponseEntity
        .internalServerError()
        .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
  }
}
