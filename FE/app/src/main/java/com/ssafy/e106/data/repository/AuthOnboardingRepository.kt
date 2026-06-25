package com.ssafy.e106.data.repository

import android.content.Context
import com.ssafy.e106.core.result.Result
import com.ssafy.e106.feature.auth.ConsentItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthOnboardingRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveTermsAgreement(consentItems: List<ConsentItem>): Result<Unit> {
        return try {
            prefs.edit()
                .putLong(KEY_CONSENTED_AT, System.currentTimeMillis())
                .putBoolean(KEY_REQUIRED_CONSENT_GRANTED, consentItems.filter { it.required }.all { it.checked })
                .putBoolean(KEY_OPTIONAL_CONSENT_GRANTED, consentItems.firstOrNull { !it.required }?.checked == true)
                .apply()
            Result.Success(Unit)
        } catch (exception: Exception) {
            Result.Error(exception.message ?: "Failed to save terms agreement")
        }
    }

    fun hasRequiredConsentGranted(): Boolean {
        return prefs.getBoolean(KEY_REQUIRED_CONSENT_GRANTED, false)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "auth_onboarding_prefs"
        const val KEY_CONSENTED_AT = "consented_at"
        const val KEY_REQUIRED_CONSENT_GRANTED = "required_consent_granted"
        const val KEY_OPTIONAL_CONSENT_GRANTED = "optional_consent_granted"
    }
}
