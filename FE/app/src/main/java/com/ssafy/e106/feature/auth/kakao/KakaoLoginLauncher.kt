package com.ssafy.e106.feature.auth.kakao

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.ssafy.e106.BuildConfig
import com.ssafy.e106.feature.auth.KakaoLoginErrorType
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "KakaoLoginLauncher"
private const val KAKAO_EMAIL_SCOPE = "account_email"
private const val KAKAO_EMAIL_REQUIRED_MESSAGE =
    "Kakao account email is required. Please reconnect Kakao and verify your Kakao account email."

sealed interface KakaoLoginResult {
    data class Success(val kakaoAccessToken: String) : KakaoLoginResult
    data object Cancelled : KakaoLoginResult
    data class Failure(
        val errorType: KakaoLoginErrorType,
        val message: String? = null,
    ) : KakaoLoginResult
}

fun interface KakaoLoginLauncher {
    suspend fun launch(): KakaoLoginResult
}

@Composable
fun rememberKakaoLoginLauncher(): KakaoLoginLauncher {
    val context = LocalContext.current
    return remember(context) {
        KakaoLoginLauncher {
            launchKakaoLogin(context)
        }
    }
}

private suspend fun launchKakaoLogin(context: Context): KakaoLoginResult {
    if (!hasConfiguredNativeAppKey()) {
        Log.e(TAG, "Kakao Native App Key not configured (set KAKAO_NATIVE_APP_KEY in local.properties)")
        return KakaoLoginResult.Failure(
            errorType = KakaoLoginErrorType.NativeAppKeyMissing,
            message = "Kakao native app key not configured",
        )
    }

    return try {
        suspendCancellableCoroutine { continuation ->
            fun complete(result: KakaoLoginResult) {
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            }

            val accountCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
                handleLoginCallback(context, token, error, ::complete)
            }

            runCatching {
                if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
                    UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                        when {
                            token != null -> ensureKakaoEmailAvailable(context, token, ::complete)
                            error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
                                complete(KakaoLoginResult.Cancelled)
                            }

                            error != null -> {
                                UserApiClient.instance.loginWithKakaoAccount(
                                    context = context,
                                    callback = accountCallback,
                                )
                            }

                            else -> complete(noTokenFailure("Kakao Talk login returned no token"))
                        }
                    }
                } else {
                    UserApiClient.instance.loginWithKakaoAccount(
                        context = context,
                        callback = accountCallback,
                    )
                }
            }.getOrElse { throwable ->
                Log.e(TAG, "Kakao login launch failed", throwable)
                complete(
                    KakaoLoginResult.Failure(
                        errorType = KakaoLoginErrorType.SdkError,
                        message = throwable.message ?: "Kakao login exception",
                    ),
                )
            }
        }
    } catch (throwable: Throwable) {
        Log.e(TAG, "Kakao login failed unexpectedly", throwable)
        KakaoLoginResult.Failure(
            errorType = KakaoLoginErrorType.SdkError,
            message = throwable.message ?: "Kakao login exception",
        )
    }
}

private fun noTokenFailure(message: String): KakaoLoginResult.Failure {
    return KakaoLoginResult.Failure(
        errorType = KakaoLoginErrorType.Unknown,
        message = message,
    )
}

private fun emailRequiredFailure(): KakaoLoginResult.Failure {
    return KakaoLoginResult.Failure(
        errorType = KakaoLoginErrorType.EmailRequired,
        message = KAKAO_EMAIL_REQUIRED_MESSAGE,
    )
}

private fun emailPermissionDeniedFailure(message: String?): KakaoLoginResult.Failure {
    return KakaoLoginResult.Failure(
        errorType = KakaoLoginErrorType.EmailPermissionDenied,
        message = message ?: "Failed to acquire Kakao email permission",
    )
}

private fun OAuthToken?.toLoginResult(error: Throwable?): KakaoLoginResult {
    return when {
        this != null -> KakaoLoginResult.Success(accessToken)
        error is ClientError && error.reason == ClientErrorCause.Cancelled -> KakaoLoginResult.Cancelled
        error != null -> KakaoLoginResult.Failure(
            errorType = KakaoLoginErrorType.SdkError,
            message = error.message,
        )
        else -> noTokenFailure("Kakao login returned no token")
    }
}

private fun handleLoginCallback(
    context: Context,
    token: OAuthToken?,
    error: Throwable?,
    complete: (KakaoLoginResult) -> Unit,
) {
    when {
        token != null -> ensureKakaoEmailAvailable(context, token, complete)
        error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
            complete(KakaoLoginResult.Cancelled)
        }
        error != null -> complete(
            KakaoLoginResult.Failure(
                errorType = KakaoLoginErrorType.SdkError,
                message = error.message,
            ),
        )
        else -> complete(noTokenFailure("Kakao login returned no token"))
    }
}

private fun ensureKakaoEmailAvailable(
    context: Context,
    token: OAuthToken,
    complete: (KakaoLoginResult) -> Unit,
    hasRetriedScopeRequest: Boolean = false,
) {
    UserApiClient.instance.me { user, error ->
        if (error != null) {
            Log.e(TAG, "Failed to load Kakao user profile after login", error)
            complete(
                KakaoLoginResult.Failure(
                    errorType = KakaoLoginErrorType.SdkError,
                    message = error.message ?: "Failed to load Kakao account",
                ),
            )
            return@me
        }

        val account = user?.kakaoAccount
        when {
            !account?.email.isNullOrBlank() &&
                account?.isEmailValid == true &&
                account.isEmailVerified == true -> {
                complete(KakaoLoginResult.Success(token.accessToken))
            }
            // Logged in successfully, but the Kakao account still does not expose
            // a usable email. Retry once by explicitly requesting the email scope.
            account?.emailNeedsAgreement == true || !hasRetriedScopeRequest -> {
                Log.w(
                    TAG,
                    "Kakao email missing or unusable. emailNeedsAgreement=${account?.emailNeedsAgreement}, " +
                        "isEmailValid=${account?.isEmailValid}, isEmailVerified=${account?.isEmailVerified}. " +
                        "Requesting email scope.",
                )
                requestKakaoEmailScope(context, complete)
            }
            else -> {
                Log.w(
                    TAG,
                    "Kakao account email is unavailable or not verified. Check Kakao consent item settings and the user's Kakao account email status.",
                )
                complete(emailRequiredFailure())
            }
        }
    }
}

private fun requestKakaoEmailScope(
    context: Context,
    complete: (KakaoLoginResult) -> Unit,
) {
    UserApiClient.instance.loginWithNewScopes(
        context = context,
        scopes = listOf(KAKAO_EMAIL_SCOPE),
    ) { token, error ->
        when {
            token != null -> ensureKakaoEmailAvailable(
                context = context,
                token = token,
                complete = complete,
                hasRetriedScopeRequest = true,
            )
            error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
                complete(KakaoLoginResult.Cancelled)
            }
            error != null -> {
                Log.e(TAG, "Failed to acquire Kakao email scope", error)
                // The explicit email scope request itself failed, so this is the
                // permission-denied branch that the ViewModel maps to user guidance.
                complete(emailPermissionDeniedFailure(error.message))
            }
            else -> complete(noTokenFailure("Kakao email permission request returned no token"))
        }
    }
}

private fun hasConfiguredNativeAppKey(): Boolean {
    val nativeAppKey = BuildConfig.KAKAO_NATIVE_APP_KEY.trim()
    return nativeAppKey.isNotBlank() && !isPlaceholderNativeAppKey(nativeAppKey)
}

private fun isPlaceholderNativeAppKey(value: String): Boolean {
    return value.equals("your_kakao_native_key_here", ignoreCase = true) ||
        value.equals("kakao_native_app_key_here", ignoreCase = true) ||
        value.startsWith("your_", ignoreCase = true)
}
