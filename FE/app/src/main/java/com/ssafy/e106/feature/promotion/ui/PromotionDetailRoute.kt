package com.ssafy.e106.feature.promotion.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.promotion.PromotionDetailIntent
import com.ssafy.e106.feature.promotion.PromotionDetailUiEffect
import com.ssafy.e106.feature.promotion.PromotionDetailViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PromotionDetailRoute(
    promotionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PromotionDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is PromotionDetailUiEffect.OpenExternalLink -> {
                    runCatching { uriHandler.openUri(effect.url) }
                        .onFailure {
                            Toast.makeText(context, "링크를 열 수 없어요.", Toast.LENGTH_SHORT).show()
                        }
                }

                is PromotionDetailUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(viewModel, promotionId) {
        viewModel.onIntent(PromotionDetailIntent.LoadPromotionDetail(promotionId))
    }

    PromotionDetailScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRetryLoad = { viewModel.onIntent(PromotionDetailIntent.RetryLoad) },
        onOpenSignupLink = { viewModel.onIntent(PromotionDetailIntent.OpenSignupLink) },
    )
}
