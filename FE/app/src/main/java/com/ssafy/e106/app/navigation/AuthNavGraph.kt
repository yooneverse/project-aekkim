package com.ssafy.e106.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ssafy.e106.feature.analysis.view.AnalysisLoadingRoute
import com.ssafy.e106.feature.auth.ui.OnboardingRoute
import com.ssafy.e106.feature.auth.ui.TermsRoute
import com.ssafy.e106.feature.settings.ui.MarketingConsentScreen
import com.ssafy.e106.feature.settings.ui.PrivacyPolicyScreen
import com.ssafy.e106.feature.settings.ui.ServiceTermsScreen
import com.ssafy.e106.feature.subscriptionconfirm.ui.SubscriptionConfirmRoute

fun NavGraphBuilder.authNavGraph(navController: NavController) {
    composable<AuthRoute.Onboarding> {
        OnboardingRoute(
            onNavigateToTerms = {
                navController.navigate(AuthRoute.Terms)
            },
            onNavigateToDashboard = {
                navController.navigate(HomeRoute.Dashboard) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
    }

    composable<AuthRoute.Terms> {
        TermsRoute(
            onNavigateToAnalysis = {
                navController.navigate(AuthRoute.AnalysisLoading)
            },
            onNavigateToServiceTerms = {
                navController.navigate(AuthRoute.ServiceTerms)
            },
            onNavigateToPrivacyPolicy = {
                navController.navigate(AuthRoute.PrivacyPolicy)
            },
            onNavigateToMarketingConsent = {
                navController.navigate(AuthRoute.MarketingConsent)
            },
        )
    }

    composable<AuthRoute.ServiceTerms> {
        ServiceTermsScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<AuthRoute.PrivacyPolicy> {
        PrivacyPolicyScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<AuthRoute.MarketingConsent> {
        MarketingConsentScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<AuthRoute.AnalysisLoading> {
        AnalysisLoadingRoute(
            onNavigateToSubscriptionConfirm = {
                navController.navigate(AuthRoute.SubscriptionConfirm)
            },
        )
    }

    composable<AuthRoute.SubscriptionConfirm> {
        SubscriptionConfirmRoute(
            onNavigateToDashboard = {
                navController.navigate(HomeRoute.Dashboard) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToManualMapping = {
                navController.navigate(HomeRoute.ManualMapping) {
                    launchSingleTop = true
                }
            },
        )
    }
}
