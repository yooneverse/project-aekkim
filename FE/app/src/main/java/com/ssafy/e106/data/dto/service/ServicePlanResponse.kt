package com.ssafy.e106.data.dto.service

import kotlinx.serialization.Serializable

@Serializable
data class ServicePlanResponse(
    val servicePlanId: Long,
    val planName: String,
    val billingCycle: String,
    val monthlyPrice: Int,
)
