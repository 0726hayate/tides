package com.hayate0726.tides.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.CycleRepository
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.UserPrivacyRepository
import com.hayate0726.tides.domain.PhaseCalculator
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.CalendarView
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    private val db: TidesDatabase,
    private val widgetUpdater: WidgetUpdater? = null,
    private val userPrivacyRepository: UserPrivacyRepository? = null,
) : ViewModel() {

    private val repo = CycleRepository(
        db.cycleEntryDao(),
        db.symptomEntryDao(),
        db.birthControlDao(),
        db.goalDao(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    data class UiState(
        val month: YearMonth = YearMonth.now(),
        val today: LocalDate = LocalDate.now(),
        val cycles: List<Cycle> = emptyList(),
        val symptomDays: Set<LocalDate> = emptySet(),
        val view: CalendarView = CalendarView.ALL,
        val showOvulation: Boolean = false,
        val predictedPeriodRanges: List<ClosedRange<LocalDate>> = emptyList(),
        val ovulationRanges: List<ClosedRange<LocalDate>> = emptyList(),
        val follicularRanges: List<ClosedRange<LocalDate>> = emptyList(),
        val lutealRanges: List<ClosedRange<LocalDate>> = emptyList(),
        val hormonalBc: Boolean = false,
    )

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            userPrivacyRepository?.refresh(db)
            val month = _state.value.month
            // Symptom dots only need the viewed month + 1-month buffer for
            // calendar overflow rows.
            val viewFrom = month.atDay(1).minusMonths(1)
            val viewTo = month.atEndOfMonth().plusMonths(1)
            // Cycle detection needs a wider window so forward-projected
            // predictions remain visible after the user scrolls past the
            // last logged period. Look back 12 months from the viewed month
            // (or further back, if the viewed month itself is in the past).
            val today = _state.value.today
            val earliest = minOf(viewFrom, today.minusMonths(12))
            val cycles = repo.detectCycles(earliest, viewTo)
            val symptoms = repo.symptomEntriesInRange(viewFrom, viewTo).map { it.date }.toSet()
            val show = userPrivacyRepository?.view?.value?.showOvulation == true
            val bc = db.birthControlDao().activeOnce()?.method ?: BirthControlMethod.NONE
            // Project 3 cycles ahead. We trim the FIRST cycle's period range
            // to days that have NOT been logged — the user already sees solid
            // markers for logged period days, so the dotted overlay would just
            // be visual noise on top.
            // assumeCycleLength = 28 keeps predictions visible for users
            // with only one logged period (no completed cycles yet to derive
            // a personal median from). project() prefers the personal median
            // once at least one cycle has completed.
            val projections = PhaseCalculator.project(
                cycles = cycles,
                today = today,
                birthControl = bc,
                cyclesAhead = 3,
                assumeCycleLength = 28,
            )
            val predictedPeriodRanges = projections.map { it.periodRange }
            val ovulationRanges = if (show) projections.map { it.ovulationRange } else emptyList()
            val follicularRanges = projections.mapNotNull { it.follicularRange }
            val lutealRanges = projections.mapNotNull { it.lutealRange }
            _state.value = _state.value.copy(
                cycles = cycles,
                symptomDays = symptoms,
                showOvulation = show,
                predictedPeriodRanges = predictedPeriodRanges,
                ovulationRanges = ovulationRanges,
                follicularRanges = follicularRanges,
                lutealRanges = lutealRanges,
                hormonalBc = bc.isHormonal,
            )
            widgetUpdater?.publish(cycles, showOvulation = show)
        }
    }

    fun changeView(view: CalendarView) {
        _state.value = _state.value.copy(view = view)
    }

    fun goToMonth(month: YearMonth) {
        if (month == _state.value.month) return
        _state.value = _state.value.copy(month = month)
        refresh()
    }

    fun nextMonth() = goToMonth(_state.value.month.plusMonths(1))
    fun previousMonth() = goToMonth(_state.value.month.minusMonths(1))
    fun goToToday() = goToMonth(YearMonth.from(_state.value.today))
}
