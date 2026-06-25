package com.ssafy.e106.data.dto.service

import kotlinx.serialization.Serializable

@Serializable
data class ServiceListItemResponse(
    val serviceId: Long,
    val code: String,
    val name: String,
    val logoUrl: String? = null,
)
