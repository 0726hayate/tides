package com.hayate0726.tides.notifications

/**
 * Reminder kinds. v1 shipped three (PERIOD_PREDICTED / PERIOD_START / LATE_PERIOD)
 * keyed off the next predicted period. v1.5 adds three more:
 *
 *  - FERTILE_WINDOW_OPEN — for users tracking ovulation. Suppressed on hormonal
 *    BC (no ovulation → meaningless notification).
 *  - PMS_CHECKIN — prompts logging in the run-up to a predicted period.
 *  - CYCLE_COMPLETE_SUMMARY — fires shortly after a predicted period start so
 *    the user can reflect on the cycle that just closed; the body is generic
 *    so the receiver doesn't need to read the encrypted DB.
 *
 * Body strings live here (not in strings.xml) so the schedule calculator can
 * be unit-tested without an Android context. Localization moves to resources
 * when the translation set is real.
 *
 * Each reminder has its own AlarmManager requestCode so scheduling one type
 * doesn't replace another type's pending alarm.
 */
enum class ReminderType(val body: String, val requestCode: Int) {
    PERIOD_PREDICTED(
        body = "Period predicted in 3 days.",
        requestCode = 1001,
    ),
    PERIOD_START(
        body = "Time to log? Period predicted today.",
        requestCode = 1002,
    ),
    LATE_PERIOD(
        body = "Your period is 3 days later than predicted.",
        requestCode = 1003,
    ),
    FERTILE_WINDOW_OPEN(
        body = "Fertile window likely opens tomorrow.",
        requestCode = 1004,
    ),
    PMS_CHECKIN(
        body = "Period likely in about 5 days — log how you feel.",
        requestCode = 1005,
    ),
    CYCLE_COMPLETE_SUMMARY(
        body = "New cycle started. Tap to see your last cycle in Stats.",
        requestCode = 1006,
    ),
}
