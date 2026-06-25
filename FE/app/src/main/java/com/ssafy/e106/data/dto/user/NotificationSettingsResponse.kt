package com.ssafy.e106.data.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettingsResponse(
    val checkinAlertEnabled: Boolean,
    val promoAlertEnabled: Boolean,
)
