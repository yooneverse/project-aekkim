package com.ssafy.e106.data.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class OptionalConsentRequest(
    val optionalConsentAgreed: Boolean,
)
