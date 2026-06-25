package com.ssafy.e106.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.BuildConfig
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.repository.AuthOnboardingRepository
import com.ssafy.e106.data.repository.AuthRepository
import com.ssafy.e106.data.repository.TokenRepository
import com.ssafy.e106.data.repository.UserRepository
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
class MyPageViewModel @Inject constructor(
    private val authOnboardingRepository: AuthOnboardingRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val tokenRepository: TokenRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MyPageUiState(
            appVersion = "v${BuildConfig.VERSION_NAME}",
        ),
    )
    val uiState: StateFlow<MyPageUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<MyPageUiEffect>(replay = 0)
    val uiEffect: SharedFlow<MyPageUiEffect> = _uiEffect.asSharedFlow()

    private var hasLoadedProfile = false

    fun onIntent(intent: MyPageIntent) {
        when (intent) {
            MyPageIntent.LoadProfile -> loadProfile(forceRefresh = false)
            MyPageIntent.RetryLoad -> loadProfile(forceRefresh = true)
            is MyPageIntent.UpdateAppNotificationPermission -> {
                _uiState.update { state -> state.copy(appNotificationsEnabled = intent.enabled) }
            }

            is MyPageIntent.ToggleCheckinAlert -> saveNotificationSettings(
                checkinAlertEnabled = intent.enabled,
                promoAlertEnabled = _uiState.value.promoAlertEnabled,
            )

            is MyPageIntent.TogglePromoAlert -> saveNotificationSettings(
                checkinAlertEnabled = _uiState.value.checkinAlertEnabled,
                promoAlertEnabled = intent.enabled,
            )

            is MyPageIntent.ToggleOptionalConsent -> saveOptionalConsent(intent.agreed)

            MyPageIntent.OpenTerms -> navigateToServiceTerms()
            MyPageIntent.OpenPrivacyPolicy -> navigateToPrivacyPolicy()
            MyPageIntent.OpenMarketingConsent -> navigateToMarketingConsent()
            MyPageIntent.OpenInquiry -> openExternalLink(INQUIRY_URL)
            MyPageIntent.LogoutClicked -> {
                _uiState.update { state -> state.copy(showLogoutDialog = true) }
            }

            MyPageIntent.DismissLogoutDialog -> {
                if (_uiState.value.isLoggingOut) return
                _uiState.update { state -> state.copy(showLogoutDialog = false) }
            }

            MyPageIntent.ConfirmLogout -> confirmLogout()
            MyPageIntent.DeleteAccountClicked -> {
                if (_uiState.value.isLoggingOut) return
                navigateToDeleteAccount()
            }
        }
    }

    private fun loadProfile(forceRefresh: Boolean) {
        if (hasLoadedProfile && !forceRefresh) return

        _uiState.update { state ->
            state.copy(
                isLoading = true,
                error = if (forceRefresh) null else state.error,
            )
        }

        viewModelScope.launch {
            when (val result = userRepository.getMe()) {
                is Result.Success -> {
                    hasLoadedProfile = true
                    val profile = result.data
                    val providerLabel = resolveProviderLabel()
                    _uiState.update { state ->
                        state.copy(
                            profileImageUrl = profile.profileImageUrl,
                            displayName = profile.displayName.ifBlank { FALLBACK_DISPLAY_NAME },
                            email = profile.email,
                            linkedProviderLabel = providerLabel,
                            checkinAlertEnabled = profile.checkinAlertEnabled,
                            promoAlertEnabled = profile.promoAlertEnabled,
                            optionalConsentAgreed = profile.optionalConsentAgreed,
                            isLoading = false,
                            error = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            displayName = state.displayName.ifBlank { FALLBACK_DISPLAY_NAME },
                            linkedProviderLabel = state.linkedProviderLabel.ifBlank { resolveProviderLabel() },
                            isLoading = false,
                            error = result.message,
                        )
                    }
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun saveNotificationSettings(
        checkinAlertEnabled: Boolean,
        promoAlertEnabled: Boolean,
    ) {
        if (_uiState.value.isSavingAlerts) return

        val previousCheckin = _uiState.value.checkinAlertEnabled
        val previousPromo = _uiState.value.promoAlertEnabled

        _uiState.update { state ->
            state.copy(
                checkinAlertEnabled = checkinAlertEnabled,
                promoAlertEnabled = promoAlertEnabled,
                isSavingAlerts = true,
            )
        }

        viewModelScope.launch {
            when (val result = userRepository.updateNotificationSettings(
                checkinAlertEnabled = checkinAlertEnabled,
                promoAlertEnabled = promoAlertEnabled,
            )) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            checkinAlertEnabled = result.data.checkinAlertEnabled,
                            promoAlertEnabled = result.data.promoAlertEnabled,
                            isSavingAlerts = false,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            checkinAlertEnabled = previousCheckin,
                            promoAlertEnabled = previousPromo,
                            isSavingAlerts = false,
                        )
                    }
                    _uiEffect.emit(MyPageUiEffect.ShowToast("설정 저장에 실패했어요"))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun saveOptionalConsent(agreed: Boolean) {
        if (_uiState.value.isSavingConsent) return

        val previousValue = _uiState.value.optionalConsentAgreed

        _uiState.update { state ->
            state.copy(
                optionalConsentAgreed = agreed,
                isSavingConsent = true,
            )
        }

        viewModelScope.launch {
            when (val result = userRepository.updateOptionalConsent(
                optionalConsentAgreed = agreed,
            )) {
                is Result.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            optionalConsentAgreed = result.data.optionalConsentAgreed,
                            isSavingConsent = false,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            optionalConsentAgreed = previousValue,
                            isSavingConsent = false,
                        )
                    }
                    _uiEffect.emit(MyPageUiEffect.ShowToast("설정 저장에 실패했어요"))
                }

                Result.Loading -> Unit
            }
        }
    }

    /**
     * provider 필드가 API 응답에 없으므로 로그인 시 저장한 로컬 값을 사용한다.
     * 로컬 값도 없으면 DEFAULT_PROVIDER_LABEL("Google")로 fallback한다.
     * BE에서 provider 필드를 추가하면 API 응답 우선으로 전환한다.
     */
    private fun resolveProviderLabel(): String {
        return tokenRepository.getLoginProvider() ?: DEFAULT_PROVIDER_LABEL
    }

    private fun navigateToServiceTerms() {
        viewModelScope.launch {
            _uiEffect.emit(MyPageUiEffect.NavigateToServiceTerms)
        }
    }

    private fun navigateToPrivacyPolicy() {
        viewModelScope.launch {
            _uiEffect.emit(MyPageUiEffect.NavigateToPrivacyPolicy)
        }
    }

    private fun navigateToMarketingConsent() {
        viewModelScope.launch {
            _uiEffect.emit(MyPageUiEffect.NavigateToMarketingConsent)
        }
    }

    private fun openExternalLink(url: String) {
        viewModelScope.launch {
            if (url.isBlank()) {
                _uiEffect.emit(MyPageUiEffect.ShowToast("아직 준비 중인 항목이에요"))
                return@launch
            }
            _uiEffect.emit(MyPageUiEffect.OpenExternalLink(url))
        }
    }

    private fun confirmLogout() {
        if (_uiState.value.isLoggingOut) return

        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoggingOut = true) }

            when (val result = authRepository.logout()) {
                is Result.Success -> {
                    tokenRepository.clearTokens()
                    authOnboardingRepository.clear()
                    _uiState.update { state ->
                        state.copy(
                            isLoggingOut = false,
                            showLogoutDialog = false,
                        )
                    }
                    _uiEffect.emit(MyPageUiEffect.NavigateToOnboarding("로그아웃되었어요"))
                }

                is Result.Error -> {
                    _uiState.update { state -> state.copy(isLoggingOut = false) }
                    _uiEffect.emit(
                        MyPageUiEffect.ShowToast(
                            result.message.ifBlank { "잠시 후 다시 시도해 주세요" },
                        ),
                    )
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun navigateToDeleteAccount() {
        viewModelScope.launch {
            _uiEffect.emit(MyPageUiEffect.NavigateToDeleteAccount)
        }
    }

    private companion object {
        const val DEFAULT_PROVIDER_LABEL = "Google"
        const val FALLBACK_DISPLAY_NAME = "사용자"

        // Story4 docs mark these destinations as pending confirmation.
        const val INQUIRY_URL = ""
    }
}
