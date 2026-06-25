package com.ssafy.e106.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.AuthOnboardingRepository
import com.ssafy.e106.data.repository.TokenRepository
import com.ssafy.e106.data.repository.UserRepository
import com.ssafy.e106.feature.auth.kakao.unlinkKakaoAccount
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

@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val authOnboardingRepository: AuthOnboardingRepository,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<DeleteAccountUiEffect>(replay = 0)
    val uiEffect: SharedFlow<DeleteAccountUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: DeleteAccountIntent) {
        when (intent) {
            DeleteAccountIntent.NextStep -> moveToNextStep()
            DeleteAccountIntent.PreviousStep -> moveToPreviousStep()
            is DeleteAccountIntent.ToggleFinalCheck -> {
                _uiState.update { state -> state.copy(finalCheckAgreed = intent.agreed) }
            }

            DeleteAccountIntent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun moveToNextStep() {
        val next = when (_uiState.value.step) {
            DeleteAccountStep.Warning -> DeleteAccountStep.Confirm
            DeleteAccountStep.Confirm -> return
        }
        _uiState.update { state -> state.copy(step = next) }
    }

    private fun moveToPreviousStep() {
        when (_uiState.value.step) {
            DeleteAccountStep.Warning -> {
                viewModelScope.launch {
                    _uiEffect.emit(DeleteAccountUiEffect.NavigateBack)
                }
            }

            DeleteAccountStep.Confirm -> {
                _uiState.update { state ->
                    state.copy(step = DeleteAccountStep.Warning, finalCheckAgreed = false)
                }
            }
        }
    }

    private fun confirmDelete() {
        if (_uiState.value.isDeleting || !_uiState.value.finalCheckAgreed) return

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isDeleting = true) }

            when (val result = userRepository.deleteMe()) {
                is Result.Success -> {
                    val unlinkFailed = shouldUnlinkKakao() && unlinkKakaoAccount().isFailure

                    tokenRepository.clearTokens()
                    authOnboardingRepository.clear()
                    _uiState.update { state -> state.copy(isDeleting = false) }

                    if (unlinkFailed) {
                        _uiEffect.emit(
                            DeleteAccountUiEffect.ShowToast(
                                "계정 삭제는 완료됐지만 카카오 연결 해제는 확인이 필요해요.",
                            ),
                        )
                    }

                    _uiEffect.emit(
                        DeleteAccountUiEffect.NavigateToOnboarding("계정이 삭제되었어요."),
                    )
                }

                is Result.Error -> {
                    _uiState.update { state -> state.copy(isDeleting = false) }

                    if (shouldResetSessionAfterDeleteFailure(result.code)) {
                        tokenRepository.clearTokens()
                        authOnboardingRepository.clear()
                        _uiEffect.emit(
                            DeleteAccountUiEffect.NavigateToOnboarding(
                                "삭제 처리 중 오류가 발생해 로그인이 풀렸어요. 다시 로그인해 상태를 확인해 주세요.",
                            ),
                        )
                    } else {
                        _uiEffect.emit(
                            DeleteAccountUiEffect.ShowToast(
                                result.message.ifBlank { "계정을 삭제하지 못했어요. 잠시 후 다시 시도해 주세요." },
                            ),
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun shouldResetSessionAfterDeleteFailure(code: Int?): Boolean {
        return code == 401 || code == 403
    }

    private fun shouldUnlinkKakao(): Boolean {
        return tokenRepository.getLoginProvider().equals("Kakao", ignoreCase = true)
    }
}
