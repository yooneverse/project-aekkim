package com.ssafy.e106.data.dto.service

import kotlinx.serialization.Serializable

@Serializable
data class ServiceDetailResponse(
    val serviceId: Long,
    val code: String,
    val name: String,
    val category: String,
    val logoUrl: String? = null,
    val plans: List<ServicePlanResponse>,
)
