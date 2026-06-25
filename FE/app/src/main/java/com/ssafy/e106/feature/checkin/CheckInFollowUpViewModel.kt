package com.ssafy.e106.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * SCR-007-2 체크인 후속 권유 ViewModel.
 *
 * [SubscriptionRepository.getSubscriptionDetail]을 재사용해
 * serviceName과 monthlyPrice(절약 금액 대용)를 가져온다.
 */
@HiltViewModel
class CheckInFollowUpViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInFollowUpUiState())
    val uiState: StateFlow<CheckInFollowUpUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CheckInFollowUpUiEffect>(replay = 0)
    val uiEffect: SharedFlow<CheckInFollowUpUiEffect> = _uiEffect.asSharedFlow()

    private var loadedSubscriptionId: Long = -1L

    fun onIntent(intent: CheckInFollowUpIntent) {
        when (intent) {
            is CheckInFollowUpIntent.Load -> loadSubscription(intent.subscriptionId)
            CheckInFollowUpIntent.RetryLoad -> loadSubscription(loadedSubscriptionId)
            CheckInFollowUpIntent.GoToCancelGuide -> goToCancelGuide()
            CheckInFollowUpIntent.KeepSubscription -> keepSubscription()
        }
    }

    // ── Load ──────────────────────────────────────────────────

    private fun loadSubscription(subscriptionId: Long) {
        if (subscriptionId <= 0L) {
            _uiState.update { state ->
                state.copy(isLoading = false, error = "구독 정보를 찾을 수 없어요")
            }
            return
        }

        loadedSubscriptionId = subscriptionId
        _uiState.update { state -> state.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val result = subscriptionRepository.getSubscriptionDetail(subscriptionId)) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            serviceName = result.data.serviceName,
                            monthlyPrice = result.data.monthlyPrice,
                            error = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(isLoading = false, error = result.message)
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    // ── CTA 액션 ──────────────────────────────────────────────

    /** Primary: 아끼러 가기 → SCR-008 CancelGuide */
    private fun goToCancelGuide() {
        if (_uiState.value.isNavigating) return
        _uiState.update { state -> state.copy(isNavigating = true) }

        viewModelScope.launch {
            _uiEffect.emit(
                CheckInFollowUpUiEffect.NavigateToCancelGuide(
                    subscriptionId = loadedSubscriptionId.toString(),
                ),
            )
            // isNavigating은 화면 전환 후 자연스럽게 해제됨
        }
    }

    /** Secondary: 조금 더 유지할게요 → 대시보드 복귀 */
    private fun keepSubscription() {
        if (_uiState.value.isNavigating) return
        _uiState.update { state -> state.copy(isNavigating = true) }

        viewModelScope.launch {
            _uiEffect.emit(CheckInFollowUpUiEffect.NavigateBack)
        }
    }
}
