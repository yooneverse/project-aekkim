package com.ssafy.e106.data.interceptor

import com.ssafy.e106.data.repository.TokenRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenRepository: TokenRepository
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (shouldSkipAuthorization(originalRequest.url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val token = tokenRepository.getAccessTokenSync()
        val request = if (token.isNullOrBlank()) {
            originalRequest
        } else {
            originalRequest.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }

    private fun shouldSkipAuthorization(path: String): Boolean {
        return path.startsWith("/api/v1/auth/login/") ||
            path == "/api/v1/auth/refresh"
    }
}
