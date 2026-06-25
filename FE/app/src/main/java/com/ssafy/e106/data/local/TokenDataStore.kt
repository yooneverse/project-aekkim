package com.ssafy.e106.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ssafy.e106.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = createPreferences(context)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getTokenType(): String? = prefs.getString(KEY_TOKEN_TYPE, null)

    fun getAccessTokenExpiresAtMillis(): Long? {
        if (!prefs.contains(KEY_ACCESS_TOKEN_EXPIRES_AT_MILLIS)) return null
        return prefs.getLong(KEY_ACCESS_TOKEN_EXPIRES_AT_MILLIS, 0L)
    }

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        tokenType: String,
        accessTokenExpiresAtMillis: Long
    ) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_TOKEN_TYPE, tokenType)
            .putLong(KEY_ACCESS_TOKEN_EXPIRES_AT_MILLIS, accessTokenExpiresAtMillis)
            .apply()
    }

    fun saveLoginProvider(provider: String) {
        prefs.edit().putString(KEY_LOGIN_PROVIDER, provider).apply()
    }

    fun getLoginProvider(): String? = prefs.getString(KEY_LOGIN_PROVIDER, null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    private fun createPreferences(context: Context): SharedPreferences {
        if (shouldUsePlainPreferences()) {
            Log.w(TAG, "Using plain SharedPreferences for auth tokens on debug emulator.")
            return context.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
        }

        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrElse { throwable ->
            Log.e(TAG, "EncryptedSharedPreferences init failed. Falling back to plain SharedPreferences.", throwable)
            context.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun shouldUsePlainPreferences(): Boolean {
        if (!BuildConfig.DEBUG) return false
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.MANUFACTURER.contains("Genymotion") ||
            (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            "google_sdk" == Build.PRODUCT
    }

    private companion object {
        const val TAG = "TokenDataStore"
        const val ENCRYPTED_PREFS_NAME = "auth_prefs_encrypted"
        const val PLAIN_PREFS_NAME = "auth_prefs_debug"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_ACCESS_TOKEN_EXPIRES_AT_MILLIS = "access_token_expires_at_millis"
        const val KEY_LOGIN_PROVIDER = "login_provider"
    }
}
