package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionCreateRequest
import com.ssafy.e106.data.dto.subscription.SubscriptionCreateResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionDetailResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionListResponse
import com.ssafy.e106.data.dto.subscription.SubscriptionUpdateRequest
import com.ssafy.e106.data.dto.subscription.SubscriptionUpdateResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface SubscriptionApi {
    @GET("/api/v1/subscriptions")
    suspend fun getSubscriptions(): ApiResponse<SubscriptionListResponse>

    @GET("/api/v1/subscriptions/{subscriptionId}")
    suspend fun getSubscriptionDetail(
        @Path("subscriptionId") subscriptionId: Long,
    ): ApiResponse<SubscriptionDetailResponse>

    @POST("/api/v1/subscriptions")
    suspend fun createSubscription(
        @Body request: SubscriptionCreateRequest,
    ): ApiResponse<SubscriptionCreateResponse>

    @PATCH("/api/v1/subscriptions/{subscriptionId}")
    suspend fun updateSubscription(
        @Path("subscriptionId") subscriptionId: Long,
        @Body request: SubscriptionUpdateRequest,
    ): ApiResponse<SubscriptionUpdateResponse>

    @DELETE("/api/v1/subscriptions/{subscriptionId}")
    suspend fun deleteSubscription(
        @Path("subscriptionId") subscriptionId: Long,
    ): ApiResponse<Unit>
}
