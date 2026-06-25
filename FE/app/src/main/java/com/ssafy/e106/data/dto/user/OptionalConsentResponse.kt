package com.ssafy.e106.data.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class OptionalConsentResponse(
    val optionalConsentAgreed: Boolean,
)
