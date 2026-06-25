package com.ssafy.e106.feature.insight.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.insight.InsightIntent
import com.ssafy.e106.feature.insight.InsightViewModel

@Composable
fun InsightRoute(
    targetSubscriptionId: Long?,
    onNavigateToDashboard: () -> Unit,
    onNavigateToPromotionList: () -> Unit,
    onNavigateToMyPage: () -> Unit,
    viewModel: InsightViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        runCatching { viewModel.onIntent(InsightIntent.LoadInsights) }
            .onFailure {
                Toast.makeText(context, "인사이트 화면을 준비하지 못했어요.", Toast.LENGTH_SHORT).show()
            }
    }

    InsightScreen(
        uiState = uiState,
        targetSubscriptionId = targetSubscriptionId,
        onRetryLoad = { viewModel.onIntent(InsightIntent.RetryLoad) },
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToPromotionList = onNavigateToPromotionList,
        onNavigateToMyPage = onNavigateToMyPage,
    )
}
