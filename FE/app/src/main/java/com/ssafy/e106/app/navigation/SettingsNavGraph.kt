package com.ssafy.e106.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.ssafy.e106.feature.settings.ui.DeleteAccountRoute
import com.ssafy.e106.feature.settings.ui.MarketingConsentScreen
import com.ssafy.e106.feature.settings.ui.MyPageRoute
import com.ssafy.e106.feature.settings.ui.PrivacyPolicyScreen
import com.ssafy.e106.feature.settings.ui.ServiceTermsScreen

fun NavGraphBuilder.settingsNavGraph(navController: NavController) {
    composable<HomeRoute.MyPage> {
        MyPageRoute(
            onNavigateToDashboard = {
                navController.navigate(HomeRoute.Dashboard) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToInsight = {
                navController.navigate(HomeRoute.Insight()) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToPromotionList = {
                navController.navigate(HomeRoute.PromotionList) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToOnboarding = {
                navController.navigate(AuthRoute.Onboarding) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            onNavigateToDeleteAccount = {
                navController.navigate(HomeRoute.DeleteAccount) {
                    launchSingleTop = true
                }
            },
            onNavigateToServiceTerms = {
                navController.navigate(HomeRoute.ServiceTerms) {
                    launchSingleTop = true
                }
            },
            onNavigateToPrivacyPolicy = {
                navController.navigate(HomeRoute.PrivacyPolicy) {
                    launchSingleTop = true
                }
            },
            onNavigateToMarketingConsent = {
                navController.navigate(HomeRoute.MarketingConsent) {
                    launchSingleTop = true
                }
            },
        )
    }

    composable<HomeRoute.ServiceTerms> {
        ServiceTermsScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<HomeRoute.PrivacyPolicy> {
        PrivacyPolicyScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<HomeRoute.MarketingConsent> {
        MarketingConsentScreen(
            onNavigateBack = { navController.popBackStack() },
        )
    }

    composable<HomeRoute.DeleteAccount> {
        DeleteAccountRoute(
            onNavigateBack = { navController.popBackStack() },
            onNavigateToOnboarding = {
                navController.navigate(AuthRoute.Onboarding) {
                    popUpTo(navController.graph.id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
    }
}
