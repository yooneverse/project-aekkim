package com.ssafy.e106.feature.auth.google

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.ssafy.e106.BuildConfig

private const val TAG = "GoogleLoginLauncher"

sealed interface GoogleLoginResult {
    data class Success(val googleIdToken: String) : GoogleLoginResult
    data object Cancelled : GoogleLoginResult
    data class Failure(val message: String? = null) : GoogleLoginResult
}

fun interface GoogleLoginLauncher {
    suspend fun launch(): GoogleLoginResult
}

@Composable
fun rememberGoogleSignInLauncher(): GoogleLoginLauncher {
    val context = LocalContext.current
    val credentialManager = remember { CredentialManager.create(context) }
    return remember(credentialManager) {
        GoogleLoginLauncher {
            val serverClientId = resolveServerClientId(context)
            if (serverClientId.isNullOrBlank()) {
                Log.e(TAG, "Google Web Client ID not configured (set GOOGLE_WEB_CLIENT_ID in local.properties or add google-services.json)")
                return@GoogleLoginLauncher GoogleLoginResult.Failure("Google client ID not configured")
            }

            val signInOption = GetSignInWithGoogleOption
                .Builder(serverClientId = serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            try {
                val result = credentialManager.getCredential(context = context, request = request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    GoogleLoginResult.Success(googleIdTokenCredential.idToken)
                } else {
                    Log.w(TAG, "Unexpected credential type: ${credential.type}")
                    GoogleLoginResult.Failure("Unexpected credential type: ${credential.type}")
                }
            } catch (e: GetCredentialCancellationException) {
                GoogleLoginResult.Cancelled
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Invalid Google ID token response", e)
                GoogleLoginResult.Failure("Failed to parse Google ID token: ${e.message}")
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No Google credential available on this device", e)
                GoogleLoginResult.Failure("No Google account available on device")
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential retrieval failed: ${e.type}", e)
                val errorType = e.type.ifBlank { "unknown_credential_error" }
                val errorMessage = e.message?.takeIf { it.isNotBlank() }
                GoogleLoginResult.Failure(
                    buildString {
                        append("Credential error: ")
                        append(errorType)
                        if (errorMessage != null) {
                            append(" - ")
                            append(errorMessage)
                        }
                    },
                )
            }
        }
    }
}

/**
 * Resolves the OAuth 2.0 Web Client ID used for Google Sign-In.
 *
 * Priority:
 * 1. `default_web_client_id` string resource (generated from google-services.json by the
 *    Google Services plugin — present in CI/production builds).
 * 2. [BuildConfig.GOOGLE_WEB_CLIENT_ID] (injected from local.properties via
 *    `GOOGLE_WEB_CLIENT_ID` — useful for local development without a real google-services.json).
 *
 * Returns `null` when neither source provides a non-blank value.
 */
private fun resolveServerClientId(context: Context): String? {
    // 1. Try the resource generated from google-services.json
    val resId = context.resources.getIdentifier(
        "default_web_client_id", "string", context.packageName,
    )
    if (resId != 0) {
        val fromResource = runCatching { context.getString(resId) }.getOrNull()
        if (!fromResource.isNullOrBlank()) return fromResource
    }

    // 2. Fall back to BuildConfig field from local.properties
    val fromBuildConfig = BuildConfig.GOOGLE_WEB_CLIENT_ID
    if (fromBuildConfig.isNotBlank() && !isPlaceholderClientId(fromBuildConfig)) return fromBuildConfig

    return null
}

private fun isPlaceholderClientId(value: String): Boolean {
    return value.equals("your_google_web_client_id_here", ignoreCase = true) ||
        value.equals("google_web_client_id_here", ignoreCase = true) ||
        value.startsWith("your_", ignoreCase = true)
}
