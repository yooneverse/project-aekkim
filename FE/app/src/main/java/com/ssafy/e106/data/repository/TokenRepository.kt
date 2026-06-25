package com.ssafy.e106.data.repository

import com.ssafy.e106.data.local.TokenDataStore
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenRepository @Inject constructor(
    private val tokenDataStore: TokenDataStore
) {
    private val accessTokenRef = AtomicReference<String?>(null)
    private val refreshTokenRef = AtomicReference<String?>(null)
    private val tokenTypeRef = AtomicReference<String?>(null)
    private val accessTokenExpiresAtMillisRef = AtomicReference<Long?>(null)
    private val loginProviderRef = AtomicReference<String?>(null)

    fun initialize() {
        accessTokenRef.set(tokenDataStore.getAccessToken())
        refreshTokenRef.set(tokenDataStore.getRefreshToken())
        tokenTypeRef.set(tokenDataStore.getTokenType())
        accessTokenExpiresAtMillisRef.set(tokenDataStore.getAccessTokenExpiresAtMillis())
        loginProviderRef.set(tokenDataStore.getLoginProvider())
    }

    fun getAccessTokenSync(): String? = accessTokenRef.get()

    fun getRefreshTokenSync(): String? = refreshTokenRef.get()

    fun getTokenTypeSync(): String? = tokenTypeRef.get()

    fun getAccessTokenExpiresAtMillisSync(): Long? = accessTokenExpiresAtMillisRef.get()

    fun hasValidAccessToken(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val accessToken = getAccessTokenSync()
        if (accessToken.isNullOrBlank()) return false
        val expiresAt = getAccessTokenExpiresAtMillisSync() ?: return false
        return expiresAt > nowMillis + EXPIRY_SAFETY_WINDOW_MILLIS
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        tokenType: String,
        expiresInSeconds: Long,
        issuedAtMillis: Long = System.currentTimeMillis()
    ) {
        val expiresAtMillis = issuedAtMillis + (expiresInSeconds.coerceAtLeast(0L) * 1000L)
        accessTokenRef.set(accessToken)
        refreshTokenRef.set(refreshToken)
        tokenTypeRef.set(tokenType)
        accessTokenExpiresAtMillisRef.set(expiresAtMillis)
        tokenDataStore.saveSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            accessTokenExpiresAtMillis = expiresAtMillis
        )
    }

    fun saveLoginProvider(provider: String) {
        loginProviderRef.set(provider)
        tokenDataStore.saveLoginProvider(provider)
    }

    fun getLoginProvider(): String? = loginProviderRef.get()

    fun clearTokens() {
        accessTokenRef.set(null)
        refreshTokenRef.set(null)
        tokenTypeRef.set(null)
        accessTokenExpiresAtMillisRef.set(null)
        loginProviderRef.set(null)
        tokenDataStore.clearTokens()
    }

    private companion object {
        const val EXPIRY_SAFETY_WINDOW_MILLIS = 30_000L
    }
}
