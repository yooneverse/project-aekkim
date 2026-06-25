package com.ssafy.e106.feature.analysis.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.AnalysisHandoffRepository
import com.ssafy.e106.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class AnalysisResultUiEffect {
    data object NavigateToDashboard : AnalysisResultUiEffect()
    data object NavigateToManualMapping : AnalysisResultUiEffect()
}

@HiltViewModel
class AnalysisResultViewModel @Inject constructor(
    private val analysisHandoffRepository: AnalysisHandoffRepository,
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiEffect = MutableSharedFlow<AnalysisResultUiEffect>(replay = 0)
    val uiEffect: SharedFlow<AnalysisResultUiEffect> = _uiEffect.asSharedFlow()

    private var hasResolvedDestination = false

    fun resolveDestination() {
        if (hasResolvedDestination) return
        hasResolvedDestination = true

        viewModelScope.launch {
            val sessionResult = analysisHandoffRepository.peekAnalysisSessionResult()
            val hasPendingReviews = if (sessionResult != null) {
                subscriptionRepository.seedPendingReviewsFromAnalysisSession(sessionResult)
                sessionResult.pendingReviewSummary.items.isNotEmpty()
            } else {
                when (val pendingReviewResult = subscriptionRepository.getPendingReviews(forceRefresh = true)) {
                    is Result.Success -> pendingReviewResult.data.isNotEmpty()
                    is Result.Error -> false
                    Result.Loading -> false
                }
            }

            val effect = if (hasPendingReviews) {
                AnalysisResultUiEffect.NavigateToManualMapping
            } else {
                AnalysisResultUiEffect.NavigateToDashboard
            }
            _uiEffect.emit(effect)
        }
    }
}
