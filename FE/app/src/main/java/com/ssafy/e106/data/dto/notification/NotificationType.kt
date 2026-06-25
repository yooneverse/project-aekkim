package com.ssafy.e106.data.dto.notification

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationType {
    CHECKIN,
    CHURN_REVIEW,
    PROMO,
}
