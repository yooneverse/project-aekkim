package com.ssafy.e106.data.dto.subscriptionusage

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionUsageReportResponse(
    val summary: SubscriptionUsageReportSummaryResponse,
    val relatedInsights: List<SubscriptionUsageReportInsightResponse> = emptyList(),
    val items: List<SubscriptionUsageReportItemResponse> = emptyList(),
)
