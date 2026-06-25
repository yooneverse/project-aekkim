package com.ssafy.e106.feature.auth

data class AuthUiState(
    val currentPage: AuthPage = AuthPage.Onboarding,
    val socialLoginLoadingProvider: AuthSocialLoginProvider? = null,
    val shouldInitializeNotificationSettings: Boolean = false,
    val consentItems: List<ConsentItem> = defaultConsentItems(),
    val isAllRequiredConsented: Boolean = false,
    val isTermsSaving: Boolean = false,
    val permissionStep: PermissionStep = PermissionStep.None,
    val showNotificationPrimerDialog: Boolean = false,
    val showUsageStatsDialog: Boolean = false,
    val showUsageStatsRePrompt: Boolean = false,
) {
    val isSocialLoginLoading: Boolean
        get() = socialLoginLoadingProvider != null

    val isGoogleLoading: Boolean
        get() = socialLoginLoadingProvider == AuthSocialLoginProvider.Google

    val isKakaoLoading: Boolean
        get() = socialLoginLoadingProvider == AuthSocialLoginProvider.Kakao
}

enum class AuthPage {
    Onboarding,
    Terms,
}

enum class PermissionStep {
    None,
    Fcm,
    UsageStats,
}

enum class AuthSocialLoginProvider {
    Google,
    Kakao,
}

enum class KakaoLoginErrorType {
    EmailRequired,
    EmailPermissionDenied,
    NativeAppKeyMissing,
    SdkError,
    Unknown,
}

data class ConsentItem(
    val id: String,
    val label: String,
    val required: Boolean,
    val checked: Boolean = false,
    val detailUrl: String? = null,
)

object ConsentDetailLinks {
    const val SERVICE_TERMS = "aekkim://legal/service-terms"
    const val PRIVACY_POLICY = "aekkim://legal/privacy-policy"
    const val MARKETING_CONSENT = "aekkim://legal/marketing-consent"
}

sealed class AuthUiEffect {
    data class ShowToast(val message: String) : AuthUiEffect()
    data object LaunchGoogleLogin : AuthUiEffect()
    data object LaunchKakaoLogin : AuthUiEffect()
    data object NavigateToTerms : AuthUiEffect()
    data object NavigateToDashboard : AuthUiEffect()
    data object NavigateToAnalysis : AuthUiEffect()
    data object RequestNotificationPermission : AuthUiEffect()
    data object OpenUsageAccessSettings : AuthUiEffect()
    data class OpenConsentDetail(val url: String) : AuthUiEffect()
}

sealed class AuthIntent {
    data object GoogleLoginClicked : AuthIntent()
    data class GoogleLoginSucceeded(val googleIdToken: String) : AuthIntent()
    data object GoogleLoginBypassedForDebug : AuthIntent()
    data object GoogleLoginCancelled : AuthIntent()
    data class GoogleLoginFailed(val message: String? = null) : AuthIntent()
    data object KakaoLoginClicked : AuthIntent()
    data class KakaoLoginSucceeded(val kakaoAccessToken: String) : AuthIntent()
    data object KakaoLoginCancelled : AuthIntent()
    data class KakaoLoginFailed(
        val errorType: KakaoLoginErrorType,
        val message: String? = null,
    ) : AuthIntent()
    data class ConsentToggled(val itemId: String) : AuthIntent()
    data object AllConsentToggled : AuthIntent()
    data class ConsentDetailClicked(val itemId: String) : AuthIntent()
    data object StartClicked : AuthIntent()
    data object NotificationPermissionPromptConfirmed : AuthIntent()
    data class NotificationPermissionResult(
        val granted: Boolean,
        val usageStatsGranted: Boolean,
    ) : AuthIntent()
    data object UsageStatsAccessRequested : AuthIntent()
    data class UsageStatsPermissionUpdated(val granted: Boolean) : AuthIntent()
}

private fun defaultConsentItems(): List<ConsentItem> {
    return listOf(
        ConsentItem(
            id = "service_terms",
            label = "서비스 이용약관",
            required = true,
            detailUrl = ConsentDetailLinks.SERVICE_TERMS,
        ),
        ConsentItem(
            id = "privacy_policy",
            label = "개인정보 처리방침",
            required = true,
            detailUrl = ConsentDetailLinks.PRIVACY_POLICY,
        ),
        ConsentItem(
            id = "marketing_consent",
            label = "혜택 및 프로모션 정보 수신 동의",
            required = false,
            detailUrl = ConsentDetailLinks.MARKETING_CONSENT,
        ),
    )
}
