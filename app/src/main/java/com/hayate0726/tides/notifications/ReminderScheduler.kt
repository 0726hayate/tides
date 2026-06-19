package com.hayate0726.tides.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hayate0726.tides.domain.CyclePredictor
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.ThreatPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges [ReminderScheduleCalculator] to Android's AlarmManager.
 *
 * Uses [AlarmManager.setAndAllowWhileIdle] — inexact (±15 min on doze) but
 * doesn't require the user-prompted SCHEDULE_EXACT_ALARM permission. Period
 * reminders don't need second-level precision.
 *
 * Scheduling the same [ReminderType] twice replaces the prior alarm because
 * [PendingIntent] uses a fixed per-type request code. This is how spec §5.12's
 * "once per cycle" guarantee is enforced without persistent state — re-running
 * [refresh] after each data change keeps exactly one alarm armed per enabled
 * type, naturally bumping it forward as predictions shift.
 *
 * The receiver fires while the app may be locked, so [refresh] resolves the
 * title and lock-screen visibility from the threat preset NOW and bakes them
 * into the PendingIntent extras — the receiver itself reads nothing.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {

    /**
     * Recompute and arm/cancel alarms based on the latest cycles + prefs +
     * threat preset. Idempotent and safe to call repeatedly.
     */
    fun refresh(
        cycles: List<Cycle>,
        prefs: NotificationPreferences.Snapshot,
        preset: ThreatPreset,
        birthControl: BirthControlMethod = BirthControlMethod.NONE,
        now: ZonedDateTime = ZonedDateTime.now(),
    ) {
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val prediction = CyclePredictor.predictNextPeriod(cycles)
        val plans = ReminderScheduleCalculator.plan(
            prediction = prediction,
            prefs = prefs,
            now = now,
            isHormonalBc = birthControl.isHormonal,
        )

        val title = preset.defaultNotificationTitle
        val visibility = if (preset.lockScreenPreviewVisible) {
            NotificationCompat.VISIBILITY_PUBLIC
        } else {
            NotificationCompat.VISIBILITY_SECRET
        }

        // Cancel every type first — any type that should remain armed gets
        // re-scheduled below. Cheaper than diffing and keeps the code simple.
        for (type in ReminderType.entries) {
            alarmMgr.cancel(pendingIntentFor(type, title, visibility))
        }
        for (plan in plans) {
            alarmMgr.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                plan.triggerEpochMs,
                pendingIntentFor(plan.type, title, visibility),
            )
        }
    }

    /**
     * Cancel every reminder type. Called from duress WIPE so post-wipe
     * notifications can't fire and betray prior app use.
     */
    fun cancelAll() {
        val alarmMgr = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val title = ""
        val visibility = NotificationCompat.VISIBILITY_SECRET
        for (type in ReminderType.entries) {
            alarmMgr.cancel(pendingIntentFor(type, title, visibility))
        }
    }

    private fun pendingIntentFor(
        type: ReminderType,
        title: String,
        visibility: Int,
    ): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TYPE, type.name)
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_VISIBILITY, visibility)
        }
        return PendingIntent.getBroadcast(
            ctx,
            type.requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
