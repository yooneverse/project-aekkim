package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageReportItemResponse(
    val subscriptionId: Long,
    val subscriptionType: String,
    val serviceId: Long? = null,
    val bundleCode: String? = null,
    val serviceName: String,
    val planName: String,
    val category: String? = null,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val totalUsedMinutes: Int,
    val usedDays: Int,
    val lastUsedDate: String? = null,
    val hourlyCost: Int? = null,
    val nudgeMessage: String? = null,
)
