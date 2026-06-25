package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.subscription.CheckInCreateRequest
import com.ssafy.e106.data.dto.subscription.CheckInCreateResponse
import com.ssafy.e106.data.dto.subscription.CheckInHistoryResponse
import retrofit2.http.GET
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface CheckInApi {
    @GET("/api/v1/checkins")
    suspend fun getCheckIns(
        @Query("serviceId") serviceId: Long? = null,
        @Query("fromCycleYm") fromCycleYm: String? = null,
        @Query("toCycleYm") toCycleYm: String? = null,
        @Query("size") size: Int? = null,
    ): ApiResponse<CheckInHistoryResponse>

    @POST("/api/v1/checkins")
    suspend fun createCheckIn(
        @Body request: CheckInCreateRequest,
    ): ApiResponse<CheckInCreateResponse>
}
