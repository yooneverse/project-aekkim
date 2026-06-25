package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.subscription.BundleListResponse
import com.ssafy.e106.data.dto.service.ServiceDetailResponse
import com.ssafy.e106.data.dto.service.ServiceListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ServiceApi {
    @GET("/api/v1/services")
    suspend fun getServices(
        @Query("category") category: String,
    ): ApiResponse<ServiceListResponse>

    @GET("/api/v1/services/{serviceId}/plans")
    suspend fun getServicePlans(
        @Path("serviceId") serviceId: Long,
    ): ApiResponse<ServiceDetailResponse>

    @GET("/api/v1/bundles")
    suspend fun getBundles(): ApiResponse<BundleListResponse>
}
