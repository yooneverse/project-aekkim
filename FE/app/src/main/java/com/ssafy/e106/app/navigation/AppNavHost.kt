package com.ssafy.e106.app.navigation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(
    entryIntent: Intent? = null,
    hasAuthenticatedSession: Boolean?,
    navController: NavHostController = rememberNavController()
) {
    if (hasAuthenticatedSession == null) {
        StartupLoadingScreen()
        return
    }

    LaunchedEffect(entryIntent, hasAuthenticatedSession) {
        if (hasAuthenticatedSession) {
            entryIntent?.let { navController.handleDeepLink(it) }
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (hasAuthenticatedSession) HomeRoute.Dashboard else AuthRoute.Onboarding
    ) {
        authNavGraph(navController)
        homeNavGraph(navController)
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}
