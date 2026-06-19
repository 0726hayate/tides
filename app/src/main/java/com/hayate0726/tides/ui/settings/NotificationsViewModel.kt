package com.hayate0726.tides.ui.settings

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.ThreatPreset
import com.hayate0726.tides.notifications.NotificationPreferences
import com.hayate0726.tides.notifications.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Owns the Notifications settings screen state.
 *
 * Reads the unlocked [TidesDatabase] for cycles + threat preset; toggles
 * write through [NotificationPreferences] and trigger [ReminderScheduler]
 * to re-arm alarms. The DB is passed in rather than injected because the
 * VM only lives inside the unlocked nav subgraph — Hilt scoping it would
 * keep a stale reference after lock.
 */
class NotificationsViewModel(
    private val ctx: Context,
    private val db: TidesDatabase,
    private val prefs: NotificationPreferences,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    data class UiState(
        val periodPredictedEnabled: Boolean = false,
        val periodStartEnabled: Boolean = false,
        val latePeriodEnabled: Boolean = false,
        val fertileWindowOpenEnabled: Boolean = false,
        val pmsCheckinEnabled: Boolean = false,
        val cycleCompleteSummaryEnabled: Boolean = false,
        val systemNotificationsEnabled: Boolean = true,
        /** True when active BC is hormonal — the screen disables the fertile toggle. */
        val hormonalBc: Boolean = false,
    )

    private val _state = MutableStateFlow(snapshot())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refreshSystemPermission() {
        _state.value = _state.value.copy(
            systemNotificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
        )
    }

    fun togglePredicted(v: Boolean) {
        prefs.setPeriodPredicted(v)
        _state.value = _state.value.copy(periodPredictedEnabled = v)
        rearm()
    }

    fun toggleStart(v: Boolean) {
        prefs.setPeriodStart(v)
        _state.value = _state.value.copy(periodStartEnabled = v)
        rearm()
    }

    fun toggleLate(v: Boolean) {
        prefs.setLatePeriod(v)
        _state.value = _state.value.copy(latePeriodEnabled = v)
        rearm()
    }

    fun toggleFertileWindowOpen(v: Boolean) {
        prefs.setFertileWindowOpen(v)
        _state.value = _state.value.copy(fertileWindowOpenEnabled = v)
        rearm()
    }

    fun togglePmsCheckin(v: Boolean) {
        prefs.setPmsCheckin(v)
        _state.value = _state.value.copy(pmsCheckinEnabled = v)
        rearm()
    }

    fun toggleCycleCompleteSummary(v: Boolean) {
        prefs.setCycleCompleteSummary(v)
        _state.value = _state.value.copy(cycleCompleteSummaryEnabled = v)
        rearm()
    }

    private fun snapshot(): UiState {
        val s = prefs.snapshot()
        return UiState(
            periodPredictedEnabled = s.periodPredictedEnabled,
            periodStartEnabled = s.periodStartEnabled,
            latePeriodEnabled = s.latePeriodEnabled,
            fertileWindowOpenEnabled = s.fertileWindowOpenEnabled,
            pmsCheckinEnabled = s.pmsCheckinEnabled,
            cycleCompleteSummaryEnabled = s.cycleCompleteSummaryEnabled,
            systemNotificationsEnabled = NotificationManagerCompat.from(ctx).areNotificationsEnabled(),
            // hormonalBc is filled in asynchronously by refreshBirthControlState();
            // initial false is safe because the worst case is briefly enabling
            // a toggle that the scheduler will then refuse to arm.
            hormonalBc = false,
        )
    }

    /** Reads the active BC once on screen open to drive the fertile-toggle gate. */
    fun refreshBirthControlState() {
        viewModelScope.launch {
            val bc: BirthControlMethod = withContext(Dispatchers.IO) {
                db.birthControlDao().activeOnce()?.method ?: BirthControlMethod.NONE
            }
            _state.value = _state.value.copy(hormonalBc = bc.isHormonal)
        }
    }

    private fun rearm() {
        viewModelScope.launch {
            val (cycles, activeBc) = withContext(Dispatchers.IO) {
                val end = LocalDate.now()
                val start = end.minusYears(2)
                val entries = db.cycleEntryDao().rangeOnce(start, end)
                val bc = db.birthControlDao().activeOnce()?.method ?: BirthControlMethod.NONE
                val detected: List<Cycle> = CycleDetector.detect(
                    entries.map { CycleDetector.Entry(it.date, it.flowIntensity) },
                    activeBirthControl = bc,
                )
                detected to bc
            }
            val presetName = withContext(Dispatchers.IO) {
                db.settingsDao().get("threat_preset")
            }
            val preset = runCatching { ThreatPreset.valueOf(presetName ?: "") }
                .getOrDefault(ThreatPreset.DEFAULT)
            scheduler.refresh(cycles, prefs.snapshot(), preset, activeBc)
        }
    }
}
