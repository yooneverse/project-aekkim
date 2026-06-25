package com.ssafy.e106.feature.mapping

import com.ssafy.e106.feature.dashboard.DashboardPlanItem
import com.ssafy.e106.feature.dashboard.DashboardServiceItem
import com.ssafy.e106.feature.dashboard.ManualAddStep

data class ManualMappingUiState(
    val pendingReviews: List<PendingReviewCardItem> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val error: String? = null,
    val manualAddStep: ManualAddStep? = null,
    val selectedReviewId: Long? = null,
    val selectedReviewIds: List<Long> = emptyList(),
    val serviceOptions: List<DashboardServiceItem> = emptyList(),
    val selectedService: DashboardServiceItem? = null,
    val planOptions: List<DashboardPlanItem> = emptyList(),
    val selectedPlanId: Long? = null,
    val selectedBillingDay: Int? = null,
    val merchantGroups: List<MerchantGroupCardItem> = emptyList(),
    val expandedMerchants: Set<String> = emptySet(),
)

data class PendingReviewCardItem(
    val reviewId: Long,
    val merchantName: String,
    val suggestedServiceName: String?,
    val monthlyAmount: Int,
    val billedAtLabel: String,
    val billingDay: Int? = null,
)

data class MerchantGroupCardItem(
    val merchantName: String,
    val reviewIds: List<Long>,
    val transactions: List<PendingReviewCardItem>,
    val suggestedServiceName: String?,
)

sealed class ManualMappingUiEffect {
    data class ShowToast(val message: String) : ManualMappingUiEffect()
}

sealed class ManualMappingIntent {
    data object LoadPendingReviews : ManualMappingIntent()
    data object RetryLoad : ManualMappingIntent()
    data class OpenAddFlow(val reviewIds: List<Long> = emptyList()) : ManualMappingIntent()
    data object CloseManualAddFlow : ManualMappingIntent()
    data class SelectService(val serviceId: Long) : ManualMappingIntent()
    data class SelectPlan(val servicePlanId: Long) : ManualMappingIntent()
    data class SelectBillingDay(val day: Int) : ManualMappingIntent()
    data object SubmitManualAdd : ManualMappingIntent()
    data class RemovePendingReviews(val reviewIds: List<Long>) : ManualMappingIntent()
    data class ToggleMerchantExpand(val merchantName: String) : ManualMappingIntent()
}
