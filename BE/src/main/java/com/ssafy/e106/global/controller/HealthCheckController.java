package com.ssafy.e106.global.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.global.common.ApiResponse;

@RestController
public class HealthCheckController {

  @GetMapping("/api/v1/health")
  public ResponseEntity<ApiResponse<String>> healthCheck() {
    return ResponseEntity.ok(ApiResponse.ok("OK"));
  }
}
