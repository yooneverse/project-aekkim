package com.ssafy.e106.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.ssafy.e106.feature.cancelguide.ui.CancelGuideRoute
import com.ssafy.e106.feature.checkin.ui.CheckInFollowUpRoute
import com.ssafy.e106.feature.checkin.ui.CheckInRoute
import com.ssafy.e106.feature.dashboard.ui.DashboardRoute
import com.ssafy.e106.feature.dashboard.ui.SubscriptionDetailRoute
import com.ssafy.e106.feature.insight.ui.InsightRoute
import com.ssafy.e106.feature.mapping.ui.ManualMappingRoute
import com.ssafy.e106.feature.notification.ui.NotificationRoute
import com.ssafy.e106.feature.promotion.ui.PromotionDetailRoute
import com.ssafy.e106.feature.promotion.ui.PromotionListRoute

private const val PENDING_EDIT_SUBSCRIPTION_ID_KEY = "pendingEditSubscriptionId"

fun NavGraphBuilder.homeNavGraph(navController: NavController) {
    fun navigateToDashboardRoot() {
        val movedToExistingDashboard = navController.popBackStack(HomeRoute.Dashboard, inclusive = false)
        if (movedToExistingDashboard) {
            return
        }

        navController.navigate(HomeRoute.Dashboard) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    composable<HomeRoute.Dashboard> { backStackEntry ->
        DashboardRoute(
            onNavigateToPromotionList = {
                navController.navigate(HomeRoute.PromotionList)
            },
            onNavigateToInsight = { targetSubscriptionId ->
                navController.navigate(
                    HomeRoute.Insight(
                        targetSubscriptionId = targetSubscriptionId?.toString(),
                    ),
                ) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onNavigateToPromotionDetail = { promotionId ->
                navController.navigate(HomeRoute.PromotionDetail(id = promotionId.toString()))
            },
            pendingEditSubscriptionId = backStackEntry.savedStateHandle.get<Long>(PENDING_EDIT_SUBSCRIPTION_ID_KEY),
            onConsumePendingEditSubscription = { subscriptionId ->
                if (backStackEntry.savedStateHandle.get<Long>(PENDING_EDIT_SUBSCRIPTION_ID_KEY) == subscriptionId) {
                    backStackEntry.savedStateHandle.remove<Long>(PENDING_EDIT_SUBSCRIPTION_ID_KEY)
                }
            },
            onNavigateToNotifications = {
                navController.navigate(HomeRoute.Notifications)
            },
            onNavigateToCheckIn = { subscriptionId ->
                navController.navigate(
                    HomeRoute.CheckIn(
                        subscriptionId = subscriptionId.toString(),
                        isFullScreen = false,
                    ),
                )
            },
            onNavigateToCancelGuide = { subscriptionId ->
                navController.navigate(HomeRoute.CancelGuide(subscriptionId = subscriptionId.toString()))
            },
            onNavigateToManualMapping = {
                navController.navigate(HomeRoute.ManualMapping)
            },
            onNavigateToMyPage = {
                navController.navigate(HomeRoute.MyPage) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onAutoNavigateToFullScreenCheckIn = { subscriptionId ->
                navController.navigate(
                    HomeRoute.CheckIn(
                        subscriptionId = subscriptionId.toString(),
                        isFullScreen = true,
                    ),
                )
            },
        )
    }

    composable<HomeRoute.Insight> { backStackEntry ->
        val targetSubscriptionId = backStackEntry.arguments
            ?.getString("targetSubscriptionId")
            ?.toLongOrNull()
        InsightRoute(
            targetSubscriptionId = targetSubscriptionId,
            onNavigateToDashboard = {
                navigateToDashboardRoot()
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
            onNavigateToMyPage = {
                navController.navigate(HomeRoute.MyPage) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    composable<HomeRoute.Notifications> {
        NotificationRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
            onNavigateToCheckIn = { subscriptionId ->
                navController.navigate(
                    HomeRoute.CheckIn(
                        subscriptionId = subscriptionId.toString(),
                        isFullScreen = true,
                    ),
                )
            },
            onNavigateToSubscriptionDetail = { subscriptionId ->
                navController.navigate(HomeRoute.SubscriptionDetail(id = subscriptionId.toString()))
            },
            onNavigateToPromotionDetail = { promotionId ->
                navController.navigate(HomeRoute.PromotionDetail(id = promotionId.toString()))
            },
        )
    }

    composable<HomeRoute.SubscriptionDetail>(
        deepLinks = listOf(
            navDeepLink<HomeRoute.SubscriptionDetail>(basePath = "aekkim://subscriptions"),
        ),
    ) { backStackEntry ->
        val subscriptionId = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        SubscriptionDetailRoute(
            subscriptionId = subscriptionId,
            onDismiss = {
                navController.popBackStack()
            },
            onNavigateToEditSubscription = { targetSubscriptionId ->
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set(PENDING_EDIT_SUBSCRIPTION_ID_KEY, targetSubscriptionId)
                navigateToDashboardRoot()
            },
            onOpenPromotionDetail = { promotionId ->
                navController.navigate(HomeRoute.PromotionDetail(id = promotionId.toString()))
            },
            onOpenCancelGuide = { id ->
                navController.navigate(HomeRoute.CancelGuide(subscriptionId = id.toString()))
            },
        )
    }

    composable<HomeRoute.PromotionList> {
        PromotionListRoute(
            onNavigateToDashboard = {
                navigateToDashboardRoot()
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
            onNavigateToPromotionDetail = { promotionId ->
                navController.navigate(HomeRoute.PromotionDetail(id = promotionId.toString()))
            },
            onNavigateToMyPage = {
                navController.navigate(HomeRoute.MyPage) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }

    composable<HomeRoute.PromotionDetail>(
        deepLinks = listOf(
            navDeepLink<HomeRoute.PromotionDetail>(basePath = "aekkim://promotions"),
        ),
    ) { backStackEntry ->
        val promotionId = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: 0L
        PromotionDetailRoute(
            promotionId = promotionId,
            onNavigateBack = {
                if (!navController.popBackStack()) {
                    navController.navigate(HomeRoute.PromotionList) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        )
    }

    composable<HomeRoute.CheckIn>(
        deepLinks = listOf(
            navDeepLink<HomeRoute.CheckIn>(basePath = "aekkim://checkin"),
        ),
    ) { backStackEntry ->
        val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
        val isFullScreen = backStackEntry.arguments?.getBoolean("isFullScreen") ?: false
        CheckInRoute(
            subscriptionId = subscriptionId,
            isFullScreen = isFullScreen,
            onNavigateBack = {
                if (!navController.popBackStack()) {
                    navigateToDashboardRoot()
                }
            },
            onNavigateHome = {
                navigateToDashboardRoot()
            },
            onNavigateToFollowUp = { subId ->
                navController.navigate(HomeRoute.CheckInFollowUp(subscriptionId = subId))
            },
        )
    }

    composable<HomeRoute.CheckInFollowUp> { backStackEntry ->
        val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
        CheckInFollowUpRoute(
            subscriptionId = subscriptionId,
            onNavigateToCancelGuide = { subId ->
                navController.navigate(HomeRoute.CancelGuide(subscriptionId = subId))
            },
            onNavigateBack = {
                if (!navController.popBackStack()) {
                    navigateToDashboardRoot()
                }
            },
        )
    }

    composable<HomeRoute.CancelGuide>(
        deepLinks = listOf(
            navDeepLink<HomeRoute.CancelGuide>(basePath = "aekkim://cancel"),
        ),
    ) { backStackEntry ->
        val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
        CancelGuideRoute(
            subscriptionId = subscriptionId,
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }

    composable<HomeRoute.ManualMapping> {
        ManualMappingRoute(
            onNavigateBack = {
                navController.popBackStack()
            },
        )
    }

    settingsNavGraph(navController)
}
