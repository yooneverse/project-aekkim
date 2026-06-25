package com.ssafy.e106.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class ErrorResponse(
    val success: Boolean,
    val code: String,
    val message: String
)
