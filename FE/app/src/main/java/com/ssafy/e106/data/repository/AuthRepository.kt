package com.ssafy.e106.data.repository

import android.util.Log
import com.ssafy.e106.core.model.ApiResponse
import com.ssafy.e106.core.model.ErrorResponse
import com.ssafy.e106.core.network.currentBaseUrl
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.data.api.AuthApi
import com.ssafy.e106.data.dto.auth.GoogleLoginRequest
import com.ssafy.e106.data.dto.auth.KakaoLoginRequest
import com.ssafy.e106.data.dto.auth.LoginResponse
import com.ssafy.e106.data.dto.auth.RefreshTokenRequest
import com.ssafy.e106.data.dto.auth.TokenPair
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi
) {
    suspend fun loginWithGoogle(googleIdToken: String): Result<LoginResponse> {
        return loginWithSocial(providerLabel = "Google") {
            authApi.loginWithGoogle(GoogleLoginRequest(googleIdToken))
        }
    }

    suspend fun loginWithKakao(kakaoAccessToken: String): Result<LoginResponse> {
        return loginWithSocial(providerLabel = "Kakao") {
            authApi.loginWithKakao(KakaoLoginRequest(kakaoAccessToken))
        }
    }

    fun refreshTokenSync(refreshToken: String): TokenPair? {
        return try {
            val response = authApi.refreshTokenSync(RefreshTokenRequest(refreshToken)).execute()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                body.data
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val response = authApi.logout()
            if (response.success) {
                Result.Success(Unit)
            } else {
                Result.Error(response.message ?: "로그아웃에 실패했어요")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "로그아웃 중 오류가 발생했어요")
        }
    }

    private suspend fun loginWithSocial(
        providerLabel: String,
        request: suspend () -> ApiResponse<LoginResponse>,
    ): Result<LoginResponse> {
        return try {
            val response = request()
            if (response.success && response.data != null) {
                Result.Success(response.data)
            } else {
                Log.w(
                    TAG,
                    "$providerLabel login rejected by backend: success=${response.success}, message=${response.message}",
                )
                Result.Error(response.message ?: "$providerLabel login failed")
            }
        } catch (e: UnknownHostException) {
            Log.e(TAG, "$providerLabel login failed: backend host unresolved", e)
            Result.Error(backendUnreachableMessage())
        } catch (e: ConnectException) {
            Log.e(TAG, "$providerLabel login failed: backend connection refused", e)
            Result.Error(backendUnreachableMessage())
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "$providerLabel login failed: backend timeout", e)
            Result.Error("Backend request timed out")
        } catch (e: HttpException) {
            Log.e(TAG, "$providerLabel login failed with HTTP ${e.code()}", e)
            Result.Error(parseHttpError(e) ?: "$providerLabel login failed with server response ${e.code()}")
        } catch (e: IOException) {
            Log.e(TAG, "$providerLabel login failed with network I/O error", e)
            Result.Error(e.message ?: "Network I/O error during $providerLabel login")
        } catch (e: Exception) {
            Log.e(TAG, "$providerLabel login failed with unexpected error", e)
            Result.Error(e.message ?: "$providerLabel login exception")
        }
    }

    private fun backendUnreachableMessage(): String {
        return "Backend unreachable at ${currentBaseUrl()}"
    }

    private fun parseHttpError(exception: HttpException): String? {
        val raw = exception.response()?.errorBody()?.string()?.trim().orEmpty()
        if (raw.isBlank()) return null

        val parsed = runCatching {
            json.decodeFromString<ErrorResponse>(raw)
        }.getOrNull()

        if (parsed != null) {
            return "${parsed.code}:${parsed.message}"
        }

        return raw
    }

    private companion object {
        const val TAG = "AuthRepository"
        val json = Json { ignoreUnknownKeys = true }
    }
}
