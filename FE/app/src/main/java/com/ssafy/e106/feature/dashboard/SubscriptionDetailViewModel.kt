package com.ssafy.e106.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.PromotionRepository
import com.ssafy.e106.data.repository.SubscriptionMutationType
import com.ssafy.e106.data.repository.SubscriptionSyncEvent
import com.ssafy.e106.data.repository.SubscriptionRepository
import com.ssafy.e106.data.repository.SubscriptionUsageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionDetailRouteState(
    val detail: SubscriptionDetailUiModel? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionUsageRepository: SubscriptionUsageRepository,
    private val promotionRepository: PromotionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionDetailRouteState())
    val uiState: StateFlow<SubscriptionDetailRouteState> = _uiState.asStateFlow()

    private var loadedSubscriptionId: Long = -1L

    init {
        observeSubscriptionSync()
    }

    fun load(subscriptionId: Long) {
        loadedSubscriptionId = subscriptionId
        if (subscriptionId <= 0L) {
            _uiState.update { state ->
                state.copy(
                    detail = null,
                    isLoading = false,
                    error = "구독 정보를 찾을 수 없어요.",
                )
            }
            return
        }

        _uiState.update { state ->
            state.copy(
                detail = null,
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch {
            when (val result = subscriptionRepository.getSubscriptionDetail(subscriptionId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            detail = result.data.toSubscriptionDetailUiModel().copy(
                                isUsageDailyLoading = true,
                                usageDailyErrorMessage = null,
                            ),
                            isLoading = false,
                            error = null,
                        )
                    }
                    result.data.recommendations.firstOrNull()?.promotionId?.let { promotionId ->
                        loadRecommendationDetail(
                            subscriptionId = subscriptionId,
                            promotionId = promotionId,
                        )
                    }
                    loadUsageDaily(subscriptionId)
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            detail = null,
                            isLoading = false,
                            error = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    fun retry() {
        load(loadedSubscriptionId)
    }

    private fun loadUsageDaily(subscriptionId: Long) {
        viewModelScope.launch {
            when (
                val result = subscriptionUsageRepository.getUsageDaily(
                    subscriptionId = subscriptionId,
                )
            ) {
                is Result.Success -> {
                    _uiState.update { state ->
                        if (loadedSubscriptionId != subscriptionId) return@update state
                        val currentDetail = state.detail ?: return@update state
                        state.copy(
                            detail = currentDetail.copy(
                                usageDaily = result.data.items.toSubscriptionDetailUsagePoints(),
                                isUsageDailyLoading = false,
                                usageDailyErrorMessage = null,
                            ),
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        if (loadedSubscriptionId != subscriptionId) return@update state
                        val currentDetail = state.detail ?: return@update state
                        state.copy(
                            detail = currentDetail.copy(
                                isUsageDailyLoading = false,
                                usageDailyErrorMessage = result.message,
                            ),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun loadRecommendationDetail(
        subscriptionId: Long,
        promotionId: Long,
    ) {
        viewModelScope.launch {
            when (val result = promotionRepository.getPromotionDetail(promotionId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        if (loadedSubscriptionId != subscriptionId) return@update state
                        val currentDetail = state.detail ?: return@update state
                        state.copy(
                            detail = currentDetail.copy(
                                recommendations = currentDetail.recommendations.map { recommendation ->
                                    if (recommendation.promotionId == promotionId) {
                                        recommendation.enrich(result.data)
                                    } else {
                                        recommendation
                                    }
                                },
                            ),
                        )
                    }
                }

                is Result.Error,
                Result.Loading -> Unit
            }
        }
    }

    private fun observeSubscriptionSync() {
        viewModelScope.launch {
            subscriptionRepository.syncEvents.collectLatest { event ->
                val subscriptionChange = event as? SubscriptionSyncEvent.SubscriptionChanged ?: return@collectLatest
                if (subscriptionChange.subscriptionId != loadedSubscriptionId) return@collectLatest

                when (subscriptionChange.mutationType) {
                    SubscriptionMutationType.Created -> Unit
                    SubscriptionMutationType.Updated,
                    SubscriptionMutationType.Deleted -> load(loadedSubscriptionId)
                }
            }
        }
    }
}
