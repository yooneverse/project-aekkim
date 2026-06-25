package com.ssafy.e106.feature.auth.kakao

import android.util.Log
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "KakaoAccountManager"

suspend fun unlinkKakaoAccount(): Result<Unit> {
    return suspendCancellableCoroutine { continuation ->
        UserApiClient.instance.unlink { error ->
            val result = when {
                error == null -> {
                    Log.i(TAG, "Kakao account unlink succeeded")
                    Result.success(Unit)
                }

                error is ClientError && error.reason == ClientErrorCause.Cancelled -> {
                    Log.w(TAG, "Kakao account unlink cancelled")
                    Result.failure(IllegalStateException("Kakao unlink cancelled"))
                }

                else -> {
                    Log.e(TAG, "Kakao account unlink failed", error)
                    Result.failure(error ?: IllegalStateException("Kakao unlink failed"))
                }
            }

            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
    }
}
