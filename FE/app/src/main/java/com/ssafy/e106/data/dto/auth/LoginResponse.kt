package com.ssafy.e106.data.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val isNewUser: Boolean = false,
    val user: LoginUser
)

@Serializable
data class LoginUser(
    val userId: Long,
    val displayName: String,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val checkinAlertEnabled: Boolean,
    val promoAlertEnabled: Boolean,
    val optionalConsentAgreed: Boolean
)
