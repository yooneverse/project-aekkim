package com.ssafy.e106.feature.settings

data class MyPageUiState(
    val profileImageUrl: String? = null,
    val displayName: String = "",
    val email: String? = null,
    val linkedProviderLabel: String = "Google",
    val appNotificationsEnabled: Boolean = true,
    val checkinAlertEnabled: Boolean = false,
    val promoAlertEnabled: Boolean = false,
    val optionalConsentAgreed: Boolean = false,
    val appVersion: String = "",
    val isLoading: Boolean = true,
    val isSavingAlerts: Boolean = false,
    val isSavingConsent: Boolean = false,
    val isLoggingOut: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val error: String? = null,
)

sealed interface MyPageUiEffect {
    data class ShowToast(val message: String) : MyPageUiEffect
    data class OpenExternalLink(val url: String) : MyPageUiEffect
    data class NavigateToOnboarding(val message: String) : MyPageUiEffect
    data object NavigateToDeleteAccount : MyPageUiEffect
    data object NavigateToServiceTerms : MyPageUiEffect
    data object NavigateToPrivacyPolicy : MyPageUiEffect
    data object NavigateToMarketingConsent : MyPageUiEffect
}

sealed interface MyPageIntent {
    data object LoadProfile : MyPageIntent
    data object RetryLoad : MyPageIntent
    data class UpdateAppNotificationPermission(val enabled: Boolean) : MyPageIntent
    data class ToggleCheckinAlert(val enabled: Boolean) : MyPageIntent
    data class TogglePromoAlert(val enabled: Boolean) : MyPageIntent
    data class ToggleOptionalConsent(val agreed: Boolean) : MyPageIntent
    data object OpenTerms : MyPageIntent
    data object OpenPrivacyPolicy : MyPageIntent
    data object OpenMarketingConsent : MyPageIntent
    data object OpenInquiry : MyPageIntent
    data object LogoutClicked : MyPageIntent
    data object DismissLogoutDialog : MyPageIntent
    data object ConfirmLogout : MyPageIntent
    data object DeleteAccountClicked : MyPageIntent
}
