package com.hayate0726.tides.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hayate0726.tides.MainActivity

/**
 * Receives an exact-time alarm and posts the corresponding notification.
 *
 * Everything the receiver needs is passed via Intent extras at schedule
 * time, because the receiver fires when the app is locked and cannot read
 * the encrypted DB or the live in-memory threat preset.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val typeName = intent.getStringExtra(EXTRA_TYPE) ?: return
        val type = ReminderType.entries.firstOrNull { it.name == typeName } ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val visibility = intent.getIntExtra(EXTRA_VISIBILITY, NotificationCompat.VISIBILITY_PRIVATE)

        NotificationChannels.ensureCreated(ctx)

        val tapPendingIntent = PendingIntent.getActivity(
            ctx,
            type.requestCode,
            Intent(ctx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification: Notification = NotificationCompat.Builder(ctx, NotificationChannels.REMINDERS)
            // Using a platform drawable avoids shipping a custom asset for v1;
            // ic_popup_reminder is a monochrome bell that renders correctly on O+.
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(type.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(type.body))
            .setVisibility(visibility)
            .setAutoCancel(true)
            .setContentIntent(tapPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(type.requestCode, notification)
    }

    companion object {
        const val EXTRA_TYPE = "tides.reminder.type"
        const val EXTRA_TITLE = "tides.reminder.title"
        const val EXTRA_VISIBILITY = "tides.reminder.visibility"
    }
}
