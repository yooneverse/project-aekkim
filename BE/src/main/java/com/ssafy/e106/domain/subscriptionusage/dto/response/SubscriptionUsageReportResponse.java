package com.ssafy.e106.domain.subscriptionusage.dto.response;

import java.util.List;

public record SubscriptionUsageReportResponse(
    SubscriptionUsageReportSummaryResponse summary,
    List<SubscriptionUsageReportInsightResponse> relatedInsights,
    List<SubscriptionUsageReportItemResponse> items) {
}
