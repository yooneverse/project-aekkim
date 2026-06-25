package com.ssafy.e106.data.dto.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionListItemResponse(
    val subscriptionId: Long,
    val serviceId: Long? = null,
    val servicePlanId: Long? = null,
    val subscriptionType: String = "SINGLE",
    val bundleCode: String? = null,
    val serviceName: String,
    val planName: String? = null,
    val billingCycle: String? = null,
    val logoUrl: String? = null,
    val monthlyPrice: Int,
    val nextBillingDate: String? = null,
    val coveredServices: List<SubscriptionCoveredServiceResponse> = emptyList(),
    val expectedSavingAmount: Int? = null,
    val savingLabel: String? = null,
)
