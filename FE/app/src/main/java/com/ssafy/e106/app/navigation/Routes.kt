package com.ssafy.e106.app.navigation

import kotlinx.serialization.Serializable

sealed interface AuthRoute {
    @Serializable
    data object Onboarding : AuthRoute

    @Serializable
    data object Terms : AuthRoute

    @Serializable
    data object ServiceTerms : AuthRoute

    @Serializable
    data object PrivacyPolicy : AuthRoute

    @Serializable
    data object MarketingConsent : AuthRoute

    @Serializable
    data object AnalysisLoading : AuthRoute

    @Serializable
    data object SubscriptionConfirm : AuthRoute
}

sealed interface HomeRoute {
    @Serializable
    data object Dashboard : HomeRoute

    @Serializable
    data class Insight(
        val targetSubscriptionId: String? = null,
    ) : HomeRoute

    @Serializable
    data object Notifications : HomeRoute

    @Serializable
    data class SubscriptionDetail(val id: String) : HomeRoute

    @Serializable
    data object PromotionList : HomeRoute

    @Serializable
    data class PromotionDetail(val id: String) : HomeRoute

    @Serializable
    data class CheckIn(
        val subscriptionId: String,
        val isFullScreen: Boolean = false
    ) : HomeRoute

    @Serializable
    data class CheckInFollowUp(val subscriptionId: String) : HomeRoute

    @Serializable
    data class CancelGuide(val subscriptionId: String) : HomeRoute

    @Serializable
    data object ManualMapping : HomeRoute

    @Serializable
    data object MyPage : HomeRoute

    @Serializable
    data object ServiceTerms : HomeRoute

    @Serializable
    data object PrivacyPolicy : HomeRoute

    @Serializable
    data object MarketingConsent : HomeRoute

    @Serializable
    data object DeleteAccount : HomeRoute
}
