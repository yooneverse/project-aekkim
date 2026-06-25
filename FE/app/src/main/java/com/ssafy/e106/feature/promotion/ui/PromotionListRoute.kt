package com.ssafy.e106.feature.promotion.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.promotion.PromotionListIntent
import com.ssafy.e106.feature.promotion.PromotionListUiEffect
import com.ssafy.e106.feature.promotion.PromotionListViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PromotionListRoute(
    onNavigateToDashboard: () -> Unit,
    onNavigateToInsight: () -> Unit,
    onNavigateToPromotionDetail: (Long) -> Unit,
    onNavigateToMyPage: () -> Unit,
    viewModel: PromotionListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is PromotionListUiEffect.NavigateToPromotionDetail -> {
                    onNavigateToPromotionDetail(effect.promotionId)
                }
            }
        }
    }

    LaunchedEffect(viewModel) {
        runCatching { viewModel.onIntent(PromotionListIntent.LoadPromotions) }
            .onFailure {
                Toast.makeText(context, "추천 화면을 준비하지 못했어요.", Toast.LENGTH_SHORT).show()
            }
    }

    PromotionListScreen(
        uiState = uiState,
        onRetryLoad = { viewModel.onIntent(PromotionListIntent.RetryLoad) },
        onSelectCategory = { categoryKey ->
            viewModel.onIntent(PromotionListIntent.SelectCategory(categoryKey))
        },
        onSelectRecommendationTab = { tab ->
            viewModel.onIntent(PromotionListIntent.SelectTab(tab))
        },
        onToggleExpanded = {
            viewModel.onIntent(PromotionListIntent.ToggleExpanded)
        },
        onOpenPromotionDetail = { promotionId ->
            viewModel.onIntent(PromotionListIntent.OpenPromotionDetail(promotionId))
        },
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToInsight = onNavigateToInsight,
        onNavigateToMyPage = onNavigateToMyPage,
    )
}
