package com.ssafy.e106.data.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val userId: Long,
    val displayName: String,
    val email: String? = null,
    val profileImageUrl: String? = null,
    val checkinAlertEnabled: Boolean = true,
    val promoAlertEnabled: Boolean = true,
    val optionalConsentAgreed: Boolean = false,
    val lastLoginAt: String? = null,
)
