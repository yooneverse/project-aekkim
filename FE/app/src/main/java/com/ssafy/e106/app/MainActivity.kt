package com.ssafy.e106.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ssafy.e106.app.navigation.AppNavHost
import com.ssafy.e106.core.ui.theme.AekkimTheme
import com.ssafy.e106.data.repository.AuthRepository
import com.ssafy.e106.data.repository.TokenRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var tokenRepository: TokenRepository

    @Inject
    lateinit var authRepository: AuthRepository

    private var entryIntent: Intent? by mutableStateOf(null)
    private var hasAuthenticatedSession: Boolean? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entryIntent = intent
        enableEdgeToEdge()

        lifecycleScope.launch {
            hasAuthenticatedSession = withContext(Dispatchers.IO) {
                bootstrapSession()
            }
        }

        setContent {
            AekkimTheme {
                AppNavHost(
                    entryIntent = entryIntent,
                    hasAuthenticatedSession = hasAuthenticatedSession
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        entryIntent = intent
    }

    private fun bootstrapSession(): Boolean {
        val accessToken = tokenRepository.getAccessTokenSync()
        val refreshToken = tokenRepository.getRefreshTokenSync()

        if (accessToken.isNullOrBlank()) {
            if (refreshToken.isNullOrBlank()) {
                tokenRepository.clearTokens()
                return false
            }
            return refreshSession(refreshToken)
        }

        if (tokenRepository.hasValidAccessToken()) {
            return true
        }

        if (refreshToken.isNullOrBlank()) {
            tokenRepository.clearTokens()
            return false
        }

        return refreshSession(refreshToken)
    }

    private fun refreshSession(refreshToken: String): Boolean {
        val refreshed = authRepository.refreshTokenSync(refreshToken) ?: run {
            tokenRepository.clearTokens()
            return false
        }

        tokenRepository.saveSession(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken,
            tokenType = refreshed.tokenType,
            expiresInSeconds = refreshed.expiresIn
        )
        return true
    }
}
