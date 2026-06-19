package com.hayate0726.tides.notifications

import com.hayate0726.tides.domain.model.PredictionRange
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure-Kotlin computation of which reminders should fire and when, given a
 * prediction and the user's toggles. No Android deps; trivially unit-testable.
 *
 * Schedule decisions:
 *  - PERIOD_PREDICTED        → fires 3 days before predicted start (spec §5.12)
 *  - PERIOD_START            → fires on predicted start day             (§5.12)
 *  - LATE_PERIOD             → fires predicted-start + 3 days            (§5.12)
 *  - PMS_CHECKIN             → fires 5 days before predicted start      (v1.5)
 *  - FERTILE_WINDOW_OPEN     → fires 17 days before predicted start.    (v1.5)
 *                              Derivation: ovulation ≈ next start − 14
 *                              (PhaseCalculator's fixed-luteal model);
 *                              fertile window opens ov − 2 = next − 16;
 *                              we notify the day before, so next − 17.
 *                              Suppressed when [isHormonalBc] is true —
 *                              ovulation is anatomically absent.
 *  - CYCLE_COMPLETE_SUMMARY  → fires predicted-start + 2. By then a new   (v1.5)
 *                              period has likely been logged, so a generic
 *                              "tap to see last cycle" body is honest.
 *
 * "Once per cycle" is enforced at the AlarmManager layer: scheduling the same
 * type a second time with the same requestCode replaces the prior alarm. The
 * receiver does NOT check log state at fire time — that's a v2 hardening item
 * (it would require unlocking the DB from a BroadcastReceiver context, which
 * we don't want). v1 trades a small over-fire risk for a clean privacy story.
 */
object ReminderScheduleCalculator {

    /** Time of day each reminder fires. 09:00 in the device's local zone. */
    private val FIRE_AT_LOCAL: LocalTime = LocalTime.of(9, 0)

    data class Plan(
        val type: ReminderType,
        val triggerEpochMs: Long,
    )

    fun plan(
        prediction: PredictionRange?,
        prefs: NotificationPreferences.Snapshot,
        now: ZonedDateTime,
        isHormonalBc: Boolean = false,
    ): List<Plan> {
        if (prediction == null) return emptyList()
        val zone: ZoneId = now.zone
        val plans = mutableListOf<Plan>()

        fun maybeAdd(type: ReminderType, fireDate: LocalDate, enabled: Boolean) {
            if (!enabled) return
            val instant = fireDate.atTime(FIRE_AT_LOCAL).atZone(zone).toInstant()
            if (!instant.isAfter(now.toInstant())) return
            plans += Plan(type, instant.toEpochMilli())
        }

        val center: LocalDate = run {
            // The PredictionRange is symmetric around the center date by construction
            // in CyclePredictor (start = center - half, end = center + half). Re-derive
            // the center via midpoint to avoid coupling to that internal detail.
            val days = java.time.temporal.ChronoUnit.DAYS.between(prediction.start, prediction.end)
            prediction.start.plusDays(days / 2)
        }

        maybeAdd(ReminderType.PERIOD_PREDICTED, center.minusDays(3), prefs.periodPredictedEnabled)
        maybeAdd(ReminderType.PERIOD_START, center, prefs.periodStartEnabled)
        maybeAdd(ReminderType.LATE_PERIOD, center.plusDays(3), prefs.latePeriodEnabled)
        maybeAdd(ReminderType.PMS_CHECKIN, center.minusDays(5), prefs.pmsCheckinEnabled)
        maybeAdd(
            ReminderType.FERTILE_WINDOW_OPEN,
            center.minusDays(17),
            prefs.fertileWindowOpenEnabled && !isHormonalBc,
        )
        maybeAdd(
            ReminderType.CYCLE_COMPLETE_SUMMARY,
            center.plusDays(2),
            prefs.cycleCompleteSummaryEnabled,
        )

        return plans
    }
}
