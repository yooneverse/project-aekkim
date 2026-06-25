package com.ssafy.e106.feature.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.e106.BuildConfig
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.dto.auth.LoginResponse
import com.ssafy.e106.data.repository.AuthOnboardingRepository
import com.ssafy.e106.data.repository.AuthRepository
import com.ssafy.e106.data.repository.NotificationRepository
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
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val authOnboardingRepository: AuthOnboardingRepository,
    private val notificationRepository: NotificationRepository,
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AuthUiEffect>(replay = 0)
    val uiEffect: SharedFlow<AuthUiEffect> = _uiEffect.asSharedFlow()

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            AuthIntent.GoogleLoginClicked -> handleGoogleLoginClicked()
            is AuthIntent.GoogleLoginSucceeded -> handleGoogleLoginSucceeded(intent.googleIdToken)
            AuthIntent.GoogleLoginBypassedForDebug -> handleGoogleLoginBypassedForDebug()
            AuthIntent.GoogleLoginCancelled -> handleGoogleLoginCancelled()
            is AuthIntent.GoogleLoginFailed -> handleGoogleLoginFailed(intent.message)
            AuthIntent.KakaoLoginClicked -> handleKakaoLoginClicked()
            is AuthIntent.KakaoLoginSucceeded -> handleKakaoLoginSucceeded(intent.kakaoAccessToken)
            AuthIntent.KakaoLoginCancelled -> handleKakaoLoginCancelled()
            is AuthIntent.KakaoLoginFailed -> handleKakaoLoginFailed(intent.errorType, intent.message)
            is AuthIntent.ConsentToggled -> toggleConsent(intent.itemId)
            AuthIntent.AllConsentToggled -> toggleAllConsent()
            is AuthIntent.ConsentDetailClicked -> handleConsentDetailClicked(intent.itemId)
            AuthIntent.StartClicked -> handleStartClicked()
            AuthIntent.NotificationPermissionPromptConfirmed -> handleNotificationPermissionPromptConfirmed()
            is AuthIntent.NotificationPermissionResult ->
                handleNotificationPermissionResult(
                    granted = intent.granted,
                    usageStatsGranted = intent.usageStatsGranted,
                )
            AuthIntent.UsageStatsAccessRequested -> handleUsageStatsAccessRequested()
            is AuthIntent.UsageStatsPermissionUpdated ->
                handleUsageStatsPermissionUpdated(intent.granted)
        }
    }

    private fun handleGoogleLoginClicked() {
        launchSocialLogin(
            provider = AuthSocialLoginProvider.Google,
            effect = AuthUiEffect.LaunchGoogleLogin,
        )
    }

    private fun handleKakaoLoginClicked() {
        launchSocialLogin(
            provider = AuthSocialLoginProvider.Kakao,
            effect = AuthUiEffect.LaunchKakaoLogin,
        )
    }

    private fun launchSocialLogin(
        provider: AuthSocialLoginProvider,
        effect: AuthUiEffect,
    ) {
        if (_uiState.value.isSocialLoginLoading) return

        viewModelScope.launch {
            setSocialLoginLoadingProvider(provider)
            _uiEffect.emit(effect)
        }
    }

    private fun handleGoogleLoginSucceeded(googleIdToken: String) {
        handleSocialLoginSucceeded(
            provider = AuthSocialLoginProvider.Google,
            loginAction = { authRepository.loginWithGoogle(googleIdToken) },
        )
    }

    private fun handleKakaoLoginSucceeded(kakaoAccessToken: String) {
        handleSocialLoginSucceeded(
            provider = AuthSocialLoginProvider.Kakao,
            loginAction = { authRepository.loginWithKakao(kakaoAccessToken) },
        )
    }

    private fun handleSocialLoginSucceeded(
        provider: AuthSocialLoginProvider,
        loginAction: suspend () -> Result<LoginResponse>,
    ) {
        viewModelScope.launch {
            when (val result = loginAction()) {
                is Result.Success -> {
                    val loginResponse = result.data
                    tokenRepository.saveSession(
                        accessToken = loginResponse.accessToken,
                        refreshToken = loginResponse.refreshToken,
                        tokenType = loginResponse.tokenType,
                        expiresInSeconds = loginResponse.expiresIn,
                    )
                    tokenRepository.saveLoginProvider(provider.toStorageLabel())
                    notificationRepository.syncFcmTokenOnLoginSuccess()
                    val hasCompletedOnboarding = authOnboardingRepository.hasRequiredConsentGranted()
                    val shouldRunOnboarding = loginResponse.isNewUser && !hasCompletedOnboarding
                    _uiState.update { state ->
                        state.copy(
                            currentPage = if (shouldRunOnboarding) AuthPage.Terms else AuthPage.Onboarding,
                            socialLoginLoadingProvider = null,
                            shouldInitializeNotificationSettings = shouldRunOnboarding,
                        )
                    }
                    if (shouldRunOnboarding) {
                        _uiEffect.emit(AuthUiEffect.NavigateToTerms)
                    } else {
                        _uiEffect.emit(AuthUiEffect.NavigateToDashboard)
                    }
                }

                is Result.Error -> {
                    Log.w(TAG, "${provider.toStorageLabel()} login failed: ${result.message}")
                    val displayMessage = if (BuildConfig.DEBUG && result.message.isNotBlank()) {
                        result.message
                    } else {
                        resolveLoginErrorMessage(
                            provider = provider,
                            message = result.message,
                        )
                    }
                    setSocialLoginLoadingProvider(null)
                    _uiEffect.emit(
                        AuthUiEffect.ShowToast(displayMessage),
                    )
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun handleGoogleLoginBypassedForDebug() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    currentPage = AuthPage.Terms,
                    socialLoginLoadingProvider = null,
                    shouldInitializeNotificationSettings = false,
                )
            }
            _uiEffect.emit(AuthUiEffect.ShowToast("디버그 모드로 로그인 단계를 건너뛰었어요."))
            _uiEffect.emit(AuthUiEffect.NavigateToTerms)
        }
    }

    private fun handleGoogleLoginCancelled() {
        setSocialLoginLoadingProvider(null)
    }

    private fun handleKakaoLoginCancelled() {
        setSocialLoginLoadingProvider(null)
    }

    private fun handleGoogleLoginFailed(message: String?) {
        handleSocialLoginFailed(
            provider = AuthSocialLoginProvider.Google,
            message = message,
        )
    }

    private fun handleKakaoLoginFailed(
        errorType: KakaoLoginErrorType,
        message: String?,
    ) {
        handleSocialLoginFailed(
            provider = AuthSocialLoginProvider.Kakao,
            message = message,
            kakaoErrorType = errorType,
        )
    }

    private fun handleSocialLoginFailed(
        provider: AuthSocialLoginProvider,
        message: String?,
        kakaoErrorType: KakaoLoginErrorType? = null,
    ) {
        viewModelScope.launch {
            Log.w(TAG, "${provider.toStorageLabel()} launcher failed: ${message.orEmpty()}")
            val displayMessage = if (BuildConfig.DEBUG && !message.isNullOrBlank()) {
                message
            } else {
                resolveLoginErrorMessage(
                    provider = provider,
                    message = message,
                    kakaoErrorType = kakaoErrorType,
                )
            }
            setSocialLoginLoadingProvider(null)
            _uiEffect.emit(
                AuthUiEffect.ShowToast(displayMessage),
            )
        }
    }

    private fun toggleConsent(itemId: String) {
        _uiState.update { state ->
            val updatedItems = state.consentItems.map { item ->
                if (item.id == itemId) item.copy(checked = !item.checked) else item
            }

            state.copy(
                consentItems = updatedItems,
                isAllRequiredConsented = updatedItems.requiredItemsChecked(),
            )
        }
    }

    private fun toggleAllConsent() {
        _uiState.update { state ->
            val shouldCheckAll = state.consentItems.any { item -> !item.checked }
            val updatedItems = state.consentItems.map { item ->
                item.copy(checked = shouldCheckAll)
            }

            state.copy(
                consentItems = updatedItems,
                isAllRequiredConsented = updatedItems.requiredItemsChecked(),
            )
        }
    }

    private fun handleStartClicked() {
        viewModelScope.launch {
            val canStart = _uiState.value.isAllRequiredConsented && !_uiState.value.isTermsSaving
            if (!canStart) return@launch

            _uiState.update { state ->
                state.copy(isTermsSaving = true, permissionStep = PermissionStep.None)
            }

            when (val result = authOnboardingRepository.saveTermsAgreement(_uiState.value.consentItems)) {
                is Result.Success -> {
                    val optionalConsentAgreed = _uiState.value.consentItems
                        .firstOrNull { item -> !item.required }?.checked == true
                    syncOptionalConsentToServer(optionalConsentAgreed)

                    _uiState.update { state ->
                        state.copy(
                            isTermsSaving = false,
                            permissionStep = PermissionStep.Fcm,
                            showNotificationPrimerDialog = true,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update { state -> state.copy(isTermsSaving = false) }
                    _uiEffect.emit(AuthUiEffect.ShowToast("잠시 후 다시 시도해 주세요"))
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun handleConsentDetailClicked(itemId: String) {
        val consentItem = _uiState.value.consentItems.firstOrNull { it.id == itemId } ?: return
        viewModelScope.launch {
            val url = consentItem.detailUrl
            if (url.isNullOrBlank()) {
                _uiEffect.emit(AuthUiEffect.ShowToast("약관 페이지를 열 수 없어요. 다시 시도해 주세요"))
            } else {
                _uiEffect.emit(AuthUiEffect.OpenConsentDetail(url))
            }
        }
    }

    private fun handleNotificationPermissionPromptConfirmed() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(showNotificationPrimerDialog = false) }
            _uiEffect.emit(AuthUiEffect.RequestNotificationPermission)
        }
    }

    private fun handleNotificationPermissionResult(
        granted: Boolean,
        usageStatsGranted: Boolean,
    ) {
        syncNotificationSettingsToServer(
            granted = granted,
            initializeOnGrant = _uiState.value.shouldInitializeNotificationSettings,
        )
        _uiState.update { state ->
            state.copy(
                shouldInitializeNotificationSettings = false,
                showNotificationPrimerDialog = false,
            )
        }

        if (usageStatsGranted) {
            viewModelScope.launch {
                _uiState.update { state ->
                    state.copy(
                        permissionStep = PermissionStep.None,
                        showNotificationPrimerDialog = false,
                    )
                }
                _uiEffect.emit(AuthUiEffect.NavigateToAnalysis)
            }
            return
        }

        _uiState.update { state -> state.copy(permissionStep = PermissionStep.UsageStats) }
        showUsageStatsPrompt(rePrompt = false)
    }

    private fun syncNotificationSettingsToServer(
        granted: Boolean,
        initializeOnGrant: Boolean,
    ) {
        if (granted && !initializeOnGrant) return

        viewModelScope.launch {
            when (userRepository.updateNotificationSettings(
                checkinAlertEnabled = granted,
                promoAlertEnabled = granted,
            )) {
                is Result.Success -> Unit
                is Result.Error -> {
                    _uiEffect.emit(
                        AuthUiEffect.ShowToast(
                            "알림 설정 저장에 실패했어요. 마이페이지에서 다시 설정할 수 있어요.",
                        ),
                    )
                }

                Result.Loading -> Unit
            }
        }
    }

    private fun handleUsageStatsAccessRequested() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    showUsageStatsDialog = false,
                    showUsageStatsRePrompt = false,
                )
            }
            _uiEffect.emit(AuthUiEffect.OpenUsageAccessSettings)
        }
    }

    private fun handleUsageStatsPermissionUpdated(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                _uiState.update { state ->
                    state.copy(
                        permissionStep = PermissionStep.None,
                        showUsageStatsDialog = false,
                        showUsageStatsRePrompt = false,
                    )
                }
                _uiEffect.emit(AuthUiEffect.NavigateToAnalysis)
                return@launch
            }

            showUsageStatsPrompt(rePrompt = true)
            _uiEffect.emit(
                AuthUiEffect.ShowToast(
                    "앱 이용을 위해 필요한 권한이에요. 설정에서 허용해 주세요",
                ),
            )
        }
    }

    private fun showUsageStatsPrompt(rePrompt: Boolean) {
        _uiState.update { state ->
            state.copy(
                permissionStep = PermissionStep.UsageStats,
                showUsageStatsDialog = true,
                showUsageStatsRePrompt = rePrompt,
            )
        }
    }

    private fun setSocialLoginLoadingProvider(provider: AuthSocialLoginProvider?) {
        _uiState.update { state -> state.copy(socialLoginLoadingProvider = provider) }
    }

    /**
     * 선택 동의 값을 서버에 동기화한다 (non-blocking).
     * 로컬 저장(SharedPreferences)이 이미 완료된 상태에서 호출되므로,
     * 서버 저장 실패 시에도 온보딩 흐름은 중단하지 않는다.
     * 사용자는 마이페이지에서 언제든 다시 설정할 수 있다.
     *
     * 우선순위: 서버 저장 > 로컬 임시 저장
     * - 로컬: 즉시 저장, 오프라인 fallback
     * - 서버: authoritative source, 다음 프로필 조회 시 서버 값 우선 반영
     */
    private fun syncOptionalConsentToServer(agreed: Boolean) {
        viewModelScope.launch {
            when (userRepository.updateOptionalConsent(optionalConsentAgreed = agreed)) {
                is Result.Success -> Unit
                is Result.Error -> {
                    _uiEffect.emit(
                        AuthUiEffect.ShowToast(
                            "선택 동의 저장에 실패했어요. 마이페이지에서 다시 설정할 수 있어요.",
                        ),
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun List<ConsentItem>.requiredItemsChecked(): Boolean {
        return filter { item -> item.required }.all { item -> item.checked }
    }

    private fun resolveLoginErrorMessage(
        provider: AuthSocialLoginProvider,
        message: String?,
        kakaoErrorType: KakaoLoginErrorType? = null,
    ): String {
        val providerConflictKeywords = listOf(
            "auth_provider_conflict",
            "different social provider",
            "다른 소셜",
            "이미 다른",
        )
        val lower = message?.lowercase().orEmpty()

        return when {
            providerConflictKeywords.any { keyword -> lower.contains(keyword) } ->
                "이미 다른 소셜 계정으로 가입된 이메일이에요"

            provider == AuthSocialLoginProvider.Kakao &&
                kakaoErrorType == KakaoLoginErrorType.EmailRequired ->
                "카카오 이메일 동의가 반영되지 않았습니다. 카카오 연결 해제 후 다시 로그인하고, 계정 이메일 등록·인증 상태도 확인해주세요."

            provider == AuthSocialLoginProvider.Kakao &&
                kakaoErrorType == KakaoLoginErrorType.EmailPermissionDenied ->
                "카카오 이메일 권한 동의가 필요합니다. 동의 팝업에서 이메일 제공에 동의한 뒤 다시 시도해주세요."

            provider == AuthSocialLoginProvider.Kakao &&
                kakaoErrorType == KakaoLoginErrorType.NativeAppKeyMissing ->
                "로그인에 실패했어요. 다시 시도해 주세요"

            else -> "로그인에 실패했어요. 다시 시도해 주세요"
        }
    }

    private fun AuthSocialLoginProvider.toStorageLabel(): String {
        return when (this) {
            AuthSocialLoginProvider.Google -> PROVIDER_GOOGLE
            AuthSocialLoginProvider.Kakao -> PROVIDER_KAKAO
        }
    }

    private companion object {
        const val TAG = "AuthViewModel"
        const val PROVIDER_GOOGLE = "Google"
        const val PROVIDER_KAKAO = "Kakao"
    }
}
