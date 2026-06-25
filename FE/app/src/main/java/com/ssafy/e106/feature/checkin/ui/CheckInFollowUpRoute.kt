package com.ssafy.e106.feature.checkin.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssafy.e106.feature.checkin.CheckInFollowUpIntent
import com.ssafy.e106.feature.checkin.CheckInFollowUpUiEffect
import com.ssafy.e106.feature.checkin.CheckInFollowUpViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * SCR-007-2 체크인 후속 권유 Route.
 *
 * @param subscriptionId 대상 구독 식별자 (String → Long 파싱)
 * @param onNavigateToCancelGuide Primary CTA → SCR-008로 이동
 * @param onNavigateBack Secondary CTA / 뒤로가기 → 대시보드 복귀
 */
@Composable
fun CheckInFollowUpRoute(
    subscriptionId: String,
    onNavigateToCancelGuide: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CheckInFollowUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── 뒤로가기 = Secondary CTA (대시보드 복귀) ──
    BackHandler {
        onNavigateBack()
    }

    // ── 초기 로드 ──
    LaunchedEffect(subscriptionId) {
        val parsedId = subscriptionId.toLongOrNull() ?: -1L
        viewModel.onIntent(CheckInFollowUpIntent.Load(parsedId))
    }

    // ── Effect 핸들링 ──
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collectLatest { effect ->
            when (effect) {
                is CheckInFollowUpUiEffect.NavigateToCancelGuide -> {
                    onNavigateToCancelGuide(effect.subscriptionId)
                }

                CheckInFollowUpUiEffect.NavigateBack -> {
                    onNavigateBack()
                }
            }
        }
    }

    CheckInFollowUpScreen(
        uiState = uiState,
        onGoToCancelGuide = {
            viewModel.onIntent(CheckInFollowUpIntent.GoToCancelGuide)
        },
        onKeepSubscription = {
            viewModel.onIntent(CheckInFollowUpIntent.KeepSubscription)
        },
        onNavigateBack = onNavigateBack,
        onRetryLoad = {
            viewModel.onIntent(CheckInFollowUpIntent.RetryLoad)
        },
    )
}
