package com.ssafy.e106.domain.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ssafy.e106.global.exception.ErrorCode;
import com.ssafy.e106.global.exception.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice(assignableTypes = FinanceController.class)
public class FinanceExceptionHandler {

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e) {
    log.warn("Malformed finance request body: {}", e.getMessage());
    return ResponseEntity
        .badRequest()
        .body(ErrorResponse.of(ErrorCode.FINANCE_MEMBER_INVALID_BODY));
  }
}
