package com.ssafy.e106.feature.mapping.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.mapping.ManualMappingIntent
import com.ssafy.e106.feature.mapping.ManualMappingUiEffect
import com.ssafy.e106.feature.mapping.ManualMappingViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ManualMappingRoute(
    onNavigateBack: () -> Unit,
    viewModel: ManualMappingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is ManualMappingUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.onIntent(ManualMappingIntent.LoadPendingReviews)
    }

    ManualMappingScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetryLoad = { viewModel.onIntent(ManualMappingIntent.RetryLoad) },
        onOpenAddFlow = { reviewIds -> viewModel.onIntent(ManualMappingIntent.OpenAddFlow(reviewIds)) },
        onDismissAddFlow = { viewModel.onIntent(ManualMappingIntent.CloseManualAddFlow) },
        onSelectService = { serviceId -> viewModel.onIntent(ManualMappingIntent.SelectService(serviceId)) },
        onSelectPlan = { planId -> viewModel.onIntent(ManualMappingIntent.SelectPlan(planId)) },
        onSelectBillingDay = { day -> viewModel.onIntent(ManualMappingIntent.SelectBillingDay(day)) },
        onSubmitManualAdd = { viewModel.onIntent(ManualMappingIntent.SubmitManualAdd) },
        onRemoveReview = { reviewIds -> viewModel.onIntent(ManualMappingIntent.RemovePendingReviews(reviewIds)) },
        onToggleMerchantExpand = { merchantName -> viewModel.onIntent(ManualMappingIntent.ToggleMerchantExpand(merchantName)) },
    )
}
