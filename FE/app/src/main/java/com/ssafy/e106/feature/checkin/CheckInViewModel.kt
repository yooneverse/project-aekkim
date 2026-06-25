package com.ssafy.e106.feature.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.CheckInSubmitResult
import com.ssafy.e106.data.repository.SubscriptionDetailData
import com.ssafy.e106.data.repository.SubscriptionRepository
import com.ssafy.e106.data.repository.SubscriptionUsageEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.YearMonth
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
 * SCR-007 체크인 응답 ViewModel.
 */
@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState: StateFlow<CheckInUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<CheckInUiEffect>(replay = 0)
    val uiEffect: SharedFlow<CheckInUiEffect> = _uiEffect.asSharedFlow()

    private var loadedSubscriptionId: Long = -1L

    /** 로드된 구독 상세 — BAD 후속 권유 판단에 사용한다. */
    private var loadedDetail: SubscriptionDetailData? = null
    private var loadedCheckInHistory: List<SubscriptionUsageEntry> = emptyList()

    fun onIntent(intent: CheckInIntent) {
        when (intent) {
            is CheckInIntent.Load -> loadSubscription(intent.subscriptionId)
            CheckInIntent.RetryLoad -> loadSubscription(loadedSubscriptionId)
            CheckInIntent.RetrySubmit -> retrySubmit()
            is CheckInIntent.Submit -> submitCheckIn(intent.response)
        }
    }

    // ── Load ──────────────────────────────────────────────────

    private fun loadSubscription(subscriptionId: Long) {
        if (subscriptionId <= 0L) {
            loadedDetail = null
            loadedCheckInHistory = emptyList()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    error = "구독 정보를 찾을 수 없어요",
                    submittingResponse = null,
                    submitError = null,
                )
            }
            return
        }

        loadedSubscriptionId = subscriptionId
        loadedCheckInHistory = emptyList()
        _uiState.update { state ->
            state.copy(
                isLoading = true,
                error = null,
                submittingResponse = null,
                submitError = null,
            )
        }

        viewModelScope.launch {
            when (val result = subscriptionRepository.getSubscriptionDetail(subscriptionId)) {
                is Result.Success -> {
                    loadedDetail = result.data
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            serviceName = result.data.serviceName,
                            logoUrl = result.data.logoUrl,
                            error = null,
                            submitError = null,
                        )
                    }
                    loadCheckInHistory(subscriptionId)
                }

                is Result.Error -> {
                    loadedDetail = null
                    loadedCheckInHistory = emptyList()
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            error = result.message,
                            submittingResponse = null,
                            submitError = null,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun retrySubmit() {
        val retryResponse = _uiState.value.submitError?.retryResponse ?: return
        submitCheckIn(retryResponse)
    }

    private fun loadCheckInHistory(subscriptionId: Long) {
        viewModelScope.launch {
            when (val result = subscriptionRepository.getCheckInHistory(subscriptionId, size = CHECKIN_HISTORY_LOOKUP_SIZE)) {
                is Result.Success -> {
                    if (loadedSubscriptionId != subscriptionId) return@launch
                    loadedCheckInHistory = result.data
                }

                is Result.Error -> Unit
                Result.Loading -> Unit
            }
        }
    }

    // ── Submit ────────────────────────────────────────────────

    /**
     * 체크인 응답을 저장한다.
     *
     * 실 API: `POST /api/v1/checkins` { serviceId, cycleYm, response }
     *
     * BAD 후속 분기 (시연용):
     * - BAD 응답 저장 성공 후, 즉시 SCR-007-2로 이동한다.
     * - 그렇지 않으면 완료 토스트 → 이전 화면 복귀.
     */
    private fun submitCheckIn(response: CheckInResponse) {
        // debounce: 이미 제출 중이면 무시
        if (_uiState.value.submittingResponse != null) return

        _uiState.update { state ->
            state.copy(
                submittingResponse = response,
                submitError = null,
            )
        }

        viewModelScope.launch {
            val saveResult = subscriptionRepository.submitCheckIn(
                subscriptionId = loadedSubscriptionId,
                cycleYm = currentCycleYm(),
                response = response.name,
            )

            when (saveResult) {
                CheckInSubmitResult.Success -> {
                    handleSubmittedResponse(response)
                }

                is CheckInSubmitResult.AlreadySubmitted -> {
                    // 시연용: 이미 제출된 월이라도 재진입 시 동일한 후속 흐름을 보여준다.
                    handleSubmittedResponse(response)
                }

                is CheckInSubmitResult.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            submittingResponse = null,
                            submitError = CheckInSubmitError(
                                message = saveResult.message.ifBlank { DEFAULT_SUBMIT_FAILURE_MESSAGE },
                                retryResponse = response,
                            ),
                        )
                    }
                }
            }
        }
    }

    // ── BAD 후속 권유 판단 ────────────────────────────────────

    /**
     * 시연용 분기.
     *
     * 실제 운영에서는 직전 체크인 BAD 여부를 확인했지만,
     * 시연에서는 BAD 1회만으로도 후속 권유 화면으로 이동시킨다.
     */
    private fun shouldNavigateToFollowUp(): Boolean {
        return true
    }

    private suspend fun handleSubmittedResponse(response: CheckInResponse) {
        _uiState.update { state ->
            state.copy(
                submittingResponse = null,
                submitError = null,
            )
        }

        if (response == CheckInResponse.BAD && shouldNavigateToFollowUp()) {
            _uiEffect.emit(
                CheckInUiEffect.NavigateToFollowUp(loadedSubscriptionId),
            )
            return
        }

        _uiEffect.emit(CheckInUiEffect.ShowToast("응답이 반영되었어요"))
        _uiEffect.emit(CheckInUiEffect.NavigateBack)
    }

    private fun currentCycleYm(): String {
        return YearMonth.now().toString()
    }

    private companion object {
        const val CHECKIN_HISTORY_LOOKUP_SIZE = 1
        const val DEFAULT_SUBMIT_FAILURE_MESSAGE = "응답 저장에 실패했어요. 다시 시도해 주세요."
    }
}
