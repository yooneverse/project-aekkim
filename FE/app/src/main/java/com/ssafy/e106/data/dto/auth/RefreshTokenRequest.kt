package com.ssafy.e106.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)
