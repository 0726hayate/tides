package com.hayate0726.tides.ui.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.UserPrivacyRepository
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class BirthControlViewModel(
    private val db: TidesDatabase,
    private val userPrivacyRepository: UserPrivacyRepository,
    private val widgetUpdater: WidgetUpdater,
) : ViewModel() {

    /**
     * No separate "selected" + "current" state and no explicit Save button.
     * Tapping a row in the UI calls [setMethod], which optimistically updates
     * [current] and persists in the background. Earlier attempts to fix a
     * "selection reverts to None" report assumed a race between the async DB
     * read and the user's tap; two patches on that theory didn't take, so the
     * architecture is now auto-save — there is no separate save step that
     * could be lost.
     */
    data class UiState(
        /** Null while the initial DB read is still in flight. */
        val current: BirthControlMethod? = null,
        val saving: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val active = db.birthControlDao().activeOnce()?.method ?: BirthControlMethod.NONE
            // Don't clobber a tap that landed while the read was in flight.
            _state.update { if (it.current == null) it.copy(current = active) else it }
        }
    }

    fun setMethod(m: BirthControlMethod) {
        val prev = _state.value.current
        if (prev == m) return
        // Optimistic UI: radio updates immediately. If the DB write fails the
        // next visit to this screen will re-read the actual stored value.
        _state.update { it.copy(current = m, saving = true) }
        viewModelScope.launch {
            val ok = persist(m)
            _state.update { it.copy(saving = false, current = if (ok) m else prev) }
        }
    }

    private suspend fun persist(m: BirthControlMethod): Boolean = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            db.withTransaction {
                // Close ALL active (endDate IS NULL) rows, not just one — guards
                // against orphans left by older versions or by an interrupted
                // earlier save.
                val allActive = db.birthControlDao().all().filter { it.endDate == null }
                for (row in allActive) {
                    db.birthControlDao().update(row.copy(endDate = today.minusDays(1)))
                }
                db.birthControlDao().insert(
                    BirthControlEntity(
                        id = 0,
                        method = m,
                        startDate = today,
                        endDate = null,
                    )
                )
            }
            userPrivacyRepository.refresh(db)
            val from = YearMonth.now().atDay(1).minusMonths(1)
            val to = YearMonth.now().atEndOfMonth().plusMonths(1)
            val entries = db.cycleEntryDao().rangeOnce(from, to)
            val cycles = CycleDetector.detect(
                entries.map { CycleDetector.Entry(it.date, it.flowIntensity) },
                activeBirthControl = m,
            )
            widgetUpdater.publish(
                cycles = cycles,
                showOvulation = userPrivacyRepository.view.value.showOvulation,
                today = today,
            )
            true
        } catch (t: Throwable) {
            // Surface in logcat so a real persistence bug is visible during
            // QA. A previous "selection reverts to None" report was untraceable
            // because every failure path was silent.
            Log.e("BirthControlVM", "Failed to persist birth control = $m", t)
            false
        }
    }
}
