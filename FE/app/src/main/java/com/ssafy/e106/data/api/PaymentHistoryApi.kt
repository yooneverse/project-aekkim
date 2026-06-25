package com.ssafy.e106.data.api

import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.data.dto.payment.PaymentHistoryListResponse
import retrofit2.http.GET

interface PaymentHistoryApi {
    @GET("/api/v1/payment-history")
    suspend fun getPaymentHistory(): ApiResponse<PaymentHistoryListResponse>
}
