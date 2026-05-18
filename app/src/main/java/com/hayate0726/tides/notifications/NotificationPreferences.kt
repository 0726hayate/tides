package com.hayate0726.tides.notifications

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-type notification toggles. SharedPreferences-backed: the data isn't
 * sensitive (a tri-bit "I opted into this reminder") and lives under the
 * app's MODE_PRIVATE files. Encrypting it would be theater.
 *
 * All toggles default to OFF per spec §5.12.
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @ApplicationContext ctx: Context,
) {
    private val sp: SharedPreferences = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    data class Snapshot(
        val periodPredictedEnabled: Boolean,
        val periodStartEnabled: Boolean,
        val latePeriodEnabled: Boolean,
    ) {
        val anyEnabled: Boolean get() =
            periodPredictedEnabled || periodStartEnabled || latePeriodEnabled
    }

    private val _state = MutableStateFlow(read())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    fun snapshot(): Snapshot = _state.value

    fun setPeriodPredicted(enabled: Boolean) = update(KEY_PREDICTED, enabled) {
        it.copy(periodPredictedEnabled = enabled)
    }

    fun setPeriodStart(enabled: Boolean) = update(KEY_START, enabled) {
        it.copy(periodStartEnabled = enabled)
    }

    fun setLatePeriod(enabled: Boolean) = update(KEY_LATE, enabled) {
        it.copy(latePeriodEnabled = enabled)
    }

    private fun read(): Snapshot = Snapshot(
        periodPredictedEnabled = sp.getBoolean(KEY_PREDICTED, false),
        periodStartEnabled = sp.getBoolean(KEY_START, false),
        latePeriodEnabled = sp.getBoolean(KEY_LATE, false),
    )

    private fun update(key: String, value: Boolean, transform: (Snapshot) -> Snapshot) {
        sp.edit().putBoolean(key, value).apply()
        _state.value = transform(_state.value)
    }

    companion object {
        private const val FILE = "tides_notifications"
        private const val KEY_PREDICTED = "period_predicted"
        private const val KEY_START = "period_start"
        private const val KEY_LATE = "late_period"
    }
}
