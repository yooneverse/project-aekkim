package com.ssafy.e106.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionReminderBannerRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDismissedSignature(): String? {
        return prefs.getString(KEY_DISMISSED_SIGNATURE, null)
    }

    fun dismiss(signature: String) {
        prefs.edit()
            .putString(KEY_DISMISSED_SIGNATURE, signature)
            .apply()
    }

    fun clear() {
        prefs.edit()
            .remove(KEY_DISMISSED_SIGNATURE)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "subscription_reminder_banner_prefs"
        const val KEY_DISMISSED_SIGNATURE = "dismissed_signature"
    }
}
