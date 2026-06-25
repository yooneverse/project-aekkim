package com.ssafy.e106.data.dto.service

import kotlinx.serialization.Serializable

@Serializable
data class ServiceListResponse(
    val category: String,
    val services: List<ServiceListItemResponse>,
)
