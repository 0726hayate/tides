package com.hayate0726.tides.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.CycleRepository
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.UserPrivacyRepository
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
    )

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            userPrivacyRepository?.refresh(db)
            val month = _state.value.month
            val from = month.atDay(1).minusMonths(1)
            val to = month.atEndOfMonth().plusMonths(1)
            val cycles = repo.detectCycles(from, to)
            val symptoms = repo.symptomEntriesInRange(from, to).map { it.date }.toSet()
            val show = userPrivacyRepository?.view?.value?.showOvulation == true
            _state.value = _state.value.copy(
                cycles = cycles, symptomDays = symptoms, showOvulation = show,
            )
            widgetUpdater?.publish(cycles, showOvulation = show)
        }
    }

    fun changeView(view: CalendarView) {
        _state.value = _state.value.copy(view = view)
    }
}
