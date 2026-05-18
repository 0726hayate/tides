package com.hayate0726.tides.notifications

/**
 * The three notification kinds shipped in v1, per spec §5.12.
 *
 * Copy is strictly factual. Body strings live here (not in strings.xml) so the
 * schedule calculator can be unit-tested without an Android context — the
 * notification UI is intentionally minimal in v1; localization can move these
 * to resources later when the translation set is real.
 */
enum class ReminderType(val body: String, val requestCode: Int) {
    PERIOD_PREDICTED(body = "Period predicted in 3 days.", requestCode = 1001),
    PERIOD_START(body = "Time to log? Period predicted today.", requestCode = 1002),
    LATE_PERIOD(body = "Your period is 3 days later than predicted.", requestCode = 1003),
}
