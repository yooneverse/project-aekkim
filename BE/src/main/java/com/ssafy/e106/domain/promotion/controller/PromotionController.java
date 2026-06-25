package com.ssafy.e106.domain.promotion.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.e106.domain.promotion.dto.response.PromotionDetailResponse;
import com.ssafy.e106.domain.promotion.dto.response.PromotionRecommendationCategoryListResponse;
import com.ssafy.e106.domain.promotion.service.PromotionService;
import com.ssafy.e106.global.common.ApiResponse;
import com.ssafy.e106.global.security.JwtUserIdResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@Tag(name = "Promotions", description = "프로모션 API")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionController {

  private final PromotionService promotionService;

  @Operation(summary = "추천 프로모션 조회")
  @GetMapping("/recommendations")
  public ResponseEntity<ApiResponse<PromotionRecommendationCategoryListResponse>> getRecommendedPromotions(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = false) String promotionType,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    PromotionRecommendationCategoryListResponse response = promotionService.getRecommendedPromotions(
        extractUserId(jwt),
        promotionType,
        page,
        size);
    return ResponseEntity.ok(ApiResponse.ok(response, "추천 프로모션을 조회했습니다."));
  }

  @Operation(summary = "프로모션 상세 조회")
  @GetMapping("/{promotionId}")
  public ResponseEntity<ApiResponse<PromotionDetailResponse>> getPromotion(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long promotionId) {
    PromotionDetailResponse response = promotionService.getPromotion(promotionId);
    return ResponseEntity.ok(ApiResponse.ok(response, "프로모션 상세를 조회했습니다."));
  }

  private Long extractUserId(Jwt jwt) {
    return JwtUserIdResolver.resolve(jwt);
  }
}
