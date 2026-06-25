package com.ssafy.e106.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long
)
