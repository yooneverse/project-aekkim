package com.ssafy.e106.feature.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.SubscriptionUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class InsightViewModel @Inject constructor(
    private val subscriptionUsageRepository: SubscriptionUsageRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightUiState())
    val uiState: StateFlow<InsightUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<InsightUiEffect>(replay = 0)
    val uiEffect: SharedFlow<InsightUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoaded = false

    fun onIntent(intent: InsightIntent) {
        when (intent) {
            InsightIntent.LoadInsights -> loadInsights(forceRefresh = false)
            InsightIntent.RetryLoad -> loadInsights(forceRefresh = true)
        }
    }

    private fun loadInsights(forceRefresh: Boolean) {
        if (hasLoaded && !forceRefresh) return

        _uiState.update { state -> state.copy(screenState = InsightScreenState.Loading) }

        viewModelScope.launch {
            val reportDeferred = async { subscriptionUsageRepository.getUsageReport() }
            val dailyDeferred = async { subscriptionUsageRepository.getUsageDaily() }

            val reportResult = reportDeferred.await()
            val dailyResult = dailyDeferred.await()

            val screenState = when {
                reportResult is Result.Success && dailyResult is Result.Success -> {
                    hasLoaded = true
                    toInsightScreenState(reportResult.data, dailyResult.data)
                }

                reportResult is Result.Error -> {
                    InsightScreenState.Error(reportResult.message.ifBlank { DEFAULT_ERROR_MESSAGE })
                }

                dailyResult is Result.Error -> {
                    InsightScreenState.Error(dailyResult.message.ifBlank { DEFAULT_ERROR_MESSAGE })
                }

                else -> InsightScreenState.Error(DEFAULT_ERROR_MESSAGE)
            }

            _uiState.update { state -> state.copy(screenState = screenState) }
        }
    }

    private companion object {
        private const val DEFAULT_ERROR_MESSAGE = "인사이트를 불러오지 못했어요."
    }
}
