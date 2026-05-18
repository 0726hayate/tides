package com.hayate0726.tides.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Single channel for all v1 reminders. IMPORTANCE_DEFAULT (no sound by
 * default; the user can promote in system settings if they want vibration).
 * Lock-screen visibility on the channel is PRIVATE — under the locked threat
 * presets the per-notification visibility tightens this to SECRET; under
 * "Just for me" it relaxes to PUBLIC. See [ReminderReceiver].
 */
object NotificationChannels {

    const val REMINDERS = "tides.reminders"

    fun ensureCreated(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(REMINDERS) != null) return
        val channel = NotificationChannel(
            REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Period reminders. Strictly local — Tides has no internet permission."
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        nm.createNotificationChannel(channel)
    }
}
