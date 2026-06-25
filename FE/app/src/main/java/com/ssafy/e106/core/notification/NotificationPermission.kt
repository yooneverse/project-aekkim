package com.ssafy.e106.core.notification

import android.content.Context
import androidx.core.app.NotificationManagerCompat

fun Context.areAppNotificationsEnabled(): Boolean {
    return NotificationManagerCompat.from(this).areNotificationsEnabled()
}
