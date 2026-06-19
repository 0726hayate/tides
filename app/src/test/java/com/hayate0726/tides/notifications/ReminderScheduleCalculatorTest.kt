package com.hayate0726.tides.notifications

import com.hayate0726.tides.domain.model.PredictionRange
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderScheduleCalculatorTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private fun at(date: String, time: String = "00:00"): ZonedDateTime =
        ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), zone)

    @Test
    fun no_prediction_returns_empty() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = null,
            prefs = allOn(),
            now = at("2026-05-01"),
        )
        assertEquals(0, plans.size)
    }

    @Test
    fun all_three_types_emitted_when_enabled_and_future() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-06-01", halfWidth = 2),
            prefs = allOn(),
            now = at("2026-05-01"),
        )
        val types = plans.map { it.type }.toSet()
        assertEquals(setOf(
            ReminderType.PERIOD_PREDICTED,
            ReminderType.PERIOD_START,
            ReminderType.LATE_PERIOD,
        ), types)
    }

    @Test
    fun disabled_types_are_excluded() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-06-01", halfWidth = 2),
            prefs = NotificationPreferences.Snapshot(
                periodPredictedEnabled = true,
                periodStartEnabled = false,
                latePeriodEnabled = false,
            ),
            now = at("2026-05-01"),
        )
        assertEquals(1, plans.size)
        assertEquals(ReminderType.PERIOD_PREDICTED, plans.single().type)
    }

    @Test
    fun past_trigger_times_are_dropped() {
        // Predicted center is 2026-05-01; "now" is 2026-05-02, so PERIOD_PREDICTED
        // (which fires 3 days before, 2026-04-28 09:00 UTC) is in the past and
        // PERIOD_START (2026-05-01 09:00 UTC) is also in the past. Only LATE_PERIOD
        // (2026-05-04 09:00 UTC) survives.
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-05-01", halfWidth = 2),
            prefs = allOn(),
            now = at("2026-05-02", "10:00"),
        )
        assertEquals(1, plans.size)
        assertEquals(ReminderType.LATE_PERIOD, plans.single().type)
    }

    @Test
    fun fires_at_9am_local_for_each_type() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-06-01", halfWidth = 2),
            prefs = allOn(),
            now = at("2026-05-01"),
        )
        for (p in plans) {
            val z = java.time.Instant.ofEpochMilli(p.triggerEpochMs).atZone(zone)
            assertEquals(9, z.hour)
            assertEquals(0, z.minute)
        }
    }

    @Test
    fun trigger_dates_match_spec_offsets() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-06-01", halfWidth = 2),
            prefs = allOn(),
            now = at("2026-05-01"),
        ).associateBy { it.type }
        fun dateOf(t: ReminderType) =
            java.time.Instant.ofEpochMilli(plans.getValue(t).triggerEpochMs).atZone(zone).toLocalDate()
        assertEquals(LocalDate.parse("2026-05-29"), dateOf(ReminderType.PERIOD_PREDICTED))
        assertEquals(LocalDate.parse("2026-06-01"), dateOf(ReminderType.PERIOD_START))
        assertEquals(LocalDate.parse("2026-06-04"), dateOf(ReminderType.LATE_PERIOD))
    }

    @Test
    fun v1_5_types_fire_at_correct_offsets() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-06-01", halfWidth = 2),
            prefs = allSixOn(),
            now = at("2026-05-01"),
        )
        val dateOf = plans.associate {
            it.type to ZonedDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it.triggerEpochMs),
                zone,
            ).toLocalDate()
        }
        // PMS_CHECKIN: 5 days before predicted start (Jun 1 - 5 = May 27).
        assertEquals(LocalDate.parse("2026-05-27"), dateOf[ReminderType.PMS_CHECKIN])
        // FERTILE_WINDOW_OPEN: 17 days before predicted start (Jun 1 - 17 = May 15).
        assertEquals(LocalDate.parse("2026-05-15"), dateOf[ReminderType.FERTILE_WINDOW_OPEN])
        // CYCLE_COMPLETE_SUMMARY: 2 days after predicted start (Jun 1 + 2 = Jun 3).
        assertEquals(LocalDate.parse("2026-06-03"), dateOf[ReminderType.CYCLE_COMPLETE_SUMMARY])
    }

    @Test
    fun fertile_window_open_is_suppressed_on_hormonal_bc() {
        val plans = ReminderScheduleCalculator.plan(
            prediction = symmetricRange("2026-06-01", halfWidth = 2),
            prefs = allSixOn(),
            now = at("2026-05-01"),
            isHormonalBc = true,
        )
        val types = plans.map { it.type }.toSet()
        // FERTILE_WINDOW_OPEN is hidden; everything else still scheduled.
        assertEquals(false, types.contains(ReminderType.FERTILE_WINDOW_OPEN))
        assertEquals(true, types.contains(ReminderType.PMS_CHECKIN))
        assertEquals(true, types.contains(ReminderType.CYCLE_COMPLETE_SUMMARY))
        assertEquals(true, types.contains(ReminderType.PERIOD_PREDICTED))
    }

    private fun allOn() = NotificationPreferences.Snapshot(
        periodPredictedEnabled = true,
        periodStartEnabled = true,
        latePeriodEnabled = true,
    )

    private fun allSixOn() = NotificationPreferences.Snapshot(
        periodPredictedEnabled = true,
        periodStartEnabled = true,
        latePeriodEnabled = true,
        fertileWindowOpenEnabled = true,
        pmsCheckinEnabled = true,
        cycleCompleteSummaryEnabled = true,
    )

    private fun symmetricRange(center: String, halfWidth: Int): PredictionRange {
        val c = LocalDate.parse(center)
        return PredictionRange(
            start = c.minusDays(halfWidth.toLong()),
            end = c.plusDays(halfWidth.toLong()),
            confidence = PredictionRange.Confidence.HIGH,
        )
    }

}
