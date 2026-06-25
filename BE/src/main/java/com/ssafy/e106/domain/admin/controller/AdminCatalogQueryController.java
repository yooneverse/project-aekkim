package com.ssafy.e106.domain.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.admin.dto.response.AdminBundleViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminPromotionViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminServiceCatalogViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminSubscriptionViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminUserViewResponse;
import com.ssafy.e106.domain.admin.dto.response.AdminSubscriptionUsageViewResponse;
import com.ssafy.e106.domain.admin.service.AdminCatalogQueryService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Dev Admin Catalog Query", description = "개발 환경용 카탈로그 조회 API")
@RequestMapping("/api/v1/dev/admin")
@RequiredArgsConstructor
public class AdminCatalogQueryController {

    private final AdminCatalogQueryService adminCatalogQueryService;

    @Operation(summary = "관리자 사용자 조회")
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserViewResponse>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCatalogQueryService.getUsers(),
                "사용자 목록을 조회했습니다."));
    }

    @Operation(summary = "관리자 서비스 조회")
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<AdminServiceCatalogViewResponse>>> getServices() {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCatalogQueryService.getServices(),
                "서비스 목록을 조회했습니다."));
    }

    @Operation(summary = "관리자 구독 조회")
    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<AdminSubscriptionViewResponse>>> getSubscriptions(
            @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCatalogQueryService.getSubscriptions(userId),
                "구독 목록을 조회했습니다."));
    }

    @Operation(summary = "관리자 번들 조회")
    @GetMapping("/bundles")
    public ResponseEntity<ApiResponse<List<AdminBundleViewResponse>>> getBundles() {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCatalogQueryService.getBundles(),
                "번들 목록을 조회했습니다."));
    }

    @Operation(summary = "관리자 프로모션 조회")
    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse<List<AdminPromotionViewResponse>>> getPromotions() {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCatalogQueryService.getPromotions(),
                "프로모션 목록을 조회했습니다."));
    }

    @Operation(summary = "관리자 구독 사용량 조회")
    @GetMapping("/subscription-usages")
    public ResponseEntity<ApiResponse<List<AdminSubscriptionUsageViewResponse>>> getSubscriptionUsages() {
        return ResponseEntity.ok(ApiResponse.ok(
                adminCatalogQueryService.getSubscriptionUsages(),
                "구독 사용량 목록을 조회했습니다."));
    }
}
