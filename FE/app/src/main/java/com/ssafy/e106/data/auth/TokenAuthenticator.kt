package com.ssafy.e106.data.auth

import com.ssafy.e106.data.repository.AuthRepository
import com.ssafy.e106.data.repository.TokenRepository
import dagger.Lazy
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val authRepository: Lazy<AuthRepository>
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (shouldSkipAuthentication(response.request.url.encodedPath)) return null
        if (response.code != 401) return null
        if (responseCount(response) >= 2) return null

        synchronized(this) {
            val latestToken = tokenRepository.getAccessTokenSync()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (!latestToken.isNullOrBlank() && latestToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            val refreshToken = tokenRepository.getRefreshTokenSync() ?: return null
            val refreshed = authRepository.get().refreshTokenSync(refreshToken) ?: run {
                tokenRepository.clearTokens()
                return null
            }

            tokenRepository.saveSession(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken,
                tokenType = refreshed.tokenType,
                expiresInSeconds = refreshed.expiresIn
            )
            return response.request.newBuilder()
                .header("Authorization", "Bearer ${refreshed.accessToken}")
                .build()
        }
    }

    private fun shouldSkipAuthentication(path: String): Boolean {
        return path.startsWith("/api/v1/auth/login/") ||
            path == "/api/v1/auth/refresh"
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count += 1
            prior = prior.priorResponse
        }
        return count
    }
}
