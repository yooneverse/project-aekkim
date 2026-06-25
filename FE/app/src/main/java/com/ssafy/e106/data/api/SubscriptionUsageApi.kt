package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyUpsertRequest
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageDailyUpsertResponse
import com.ssafy.e106.data.dto.subscriptionusage.SubscriptionUsageReportResponse
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SubscriptionUsageApi {
    @POST("/api/v1/subscription-usages/daily")
    suspend fun upsertDailyUsage(
        @Body request: SubscriptionUsageDailyUpsertRequest,
    ): ApiResponse<SubscriptionUsageDailyUpsertResponse>

    @GET("/api/v1/subscription-usages/report")
    suspend fun getSubscriptionUsageReport(
        @Query("days") days: Int = 30,
    ): ApiResponse<SubscriptionUsageReportResponse>

    @GET("/api/v1/subscription-usages/daily")
    suspend fun getSubscriptionUsageDaily(
        @Query("days") days: Int = 30,
        @Query("subscriptionId") subscriptionId: Long? = null,
    ): ApiResponse<SubscriptionUsageDailyResponse>
}
