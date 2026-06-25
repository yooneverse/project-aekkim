package com.ssafy.e106.feature.dashboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.dashboard.SubscriptionDetailViewModel
import com.ssafy.e106.feature.dashboard.ui.component.SubscriptionDetailBottomSheet

@Composable
fun SubscriptionDetailRoute(
    subscriptionId: Long,
    onDismiss: () -> Unit,
    onNavigateToEditSubscription: (Long) -> Unit,
    onOpenPromotionDetail: (Long) -> Unit,
    onOpenCancelGuide: (Long) -> Unit,
    viewModel: SubscriptionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(subscriptionId) {
        viewModel.load(subscriptionId)
    }

    SubscriptionDetailBottomSheet(
        detail = uiState.detail,
        isLoading = uiState.isLoading,
        errorMessage = uiState.error,
        onRetryLoad = viewModel::retry,
        onDismiss = onDismiss,
        onOpenPromotionDetail = onOpenPromotionDetail,
        onOpenCancelGuide = onOpenCancelGuide,
        onEditSubscription = onNavigateToEditSubscription,
    )
}
