package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.promotion.PromotionDetailResponse
import com.ssafy.e106.data.dto.promotion.PromotionRecommendationCategoryListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private const val DEFAULT_PROMOTION_TYPE = "ALL"
private const val DEFAULT_PAGE = 0
private const val DEFAULT_SIZE = 3

interface PromotionApi {
    @GET("/api/v1/promotions/recommendations")
    suspend fun getRecommendations(
        @Query("promotionType") promotionType: String = DEFAULT_PROMOTION_TYPE,
        @Query("page") page: Int = DEFAULT_PAGE,
        @Query("size") size: Int = DEFAULT_SIZE,
    ): ApiResponse<PromotionRecommendationCategoryListResponse>

    @GET("/api/v1/promotions/{promotionId}")
    suspend fun getPromotionDetail(
        @Path("promotionId") promotionId: Long,
    ): ApiResponse<PromotionDetailResponse>
}
