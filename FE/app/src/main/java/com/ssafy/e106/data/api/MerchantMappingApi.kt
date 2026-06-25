package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchConfirmRequest
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchConfirmResponse
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchLookupRequest
import com.ssafy.e106.data.dto.subscription.MerchantMappingBatchLookupResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface MerchantMappingApi {
    @POST("/api/v1/merchant-mappings/batch-lookup")
    suspend fun batchLookup(
        @Body request: MerchantMappingBatchLookupRequest,
    ): ApiResponse<MerchantMappingBatchLookupResponse>

    @POST("/api/v1/merchant-mappings/batch-confirm")
    suspend fun batchConfirm(
        @Body request: MerchantMappingBatchConfirmRequest,
    ): ApiResponse<MerchantMappingBatchConfirmResponse>
}
