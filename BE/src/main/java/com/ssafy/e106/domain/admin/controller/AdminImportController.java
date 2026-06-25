package com.ssafy.e106.domain.admin.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ssafy.e106.domain.admin.dto.response.AdminSeedImportResponse;
import com.ssafy.e106.domain.admin.service.AdminSeedImportService;
import com.ssafy.e106.global.common.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Dev Admin Imports", description = "개발 환경용 seed import API")
@RequestMapping("/api/v1/dev/admin/imports")
@RequiredArgsConstructor
public class AdminImportController {

    private final AdminSeedImportService adminSeedImportService;

    @Operation(summary = "서비스 카탈로그 JSON import")
    @PostMapping(
        value = "/service-catalog",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminSeedImportResponse>> importServiceCatalog(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSeedImportService.importServiceCatalog(file),
                "서비스 카탈로그 import가 완료되었습니다."));
    }

    @Operation(summary = "프로모션 JSON import")
    @PostMapping(
        value = "/promotions",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminSeedImportResponse>> importPromotions(
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSeedImportService.importPromotions(files),
                "프로모션 import가 완료되었습니다."));
    }

    @Operation(summary = "번들 JSON import")
    @PostMapping(
        value = "/bundles",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminSeedImportResponse>> importBundles(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSeedImportService.importBundles(file),
                "번들 import가 완료되었습니다."));
    }

    @Operation(summary = "구독 사용량 JSON import")
    @PostMapping(
        value = "/subscription-usages",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AdminSeedImportResponse>> importSubscriptionUsages(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminSeedImportService.importSubscriptionUsages(file),
                "구독 사용량 import가 완료되었습니다."));
    }
}
